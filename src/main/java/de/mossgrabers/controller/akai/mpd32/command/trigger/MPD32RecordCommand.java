// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2026
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.akai.mpd32.command.trigger;

import de.mossgrabers.controller.akai.mpd32.MPD32Configuration;
import de.mossgrabers.controller.akai.mpd32.controller.MPD32ControlSurface;
import de.mossgrabers.framework.command.core.AbstractTriggerCommand;
import de.mossgrabers.framework.daw.IApplication;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.ITransport;
import de.mossgrabers.framework.daw.data.ISlot;
import de.mossgrabers.framework.daw.data.ITrack;
import de.mossgrabers.framework.daw.data.bank.ISlotBank;
import de.mossgrabers.framework.utils.ButtonEvent;

import java.util.Optional;


/**
 * Command for the record button. It is only active in the Mix view. Without Shift it stops a
 * running recording, otherwise it toggles the clip launcher overdub. With Shift it records a new
 * clip in the selected slot of the selected track.
 *
 * @author Jürgen Moßgraber
 */
public class MPD32RecordCommand extends AbstractTriggerCommand<MPD32ControlSurface, MPD32Configuration>
{
    private final ITransport   transport;
    private final IApplication application;


    /**
     * Constructor.
     *
     * @param model The model
     * @param surface The surface
     */
    public MPD32RecordCommand (final IModel model, final MPD32ControlSurface surface)
    {
        super (model, surface);
        this.transport = model.getTransport ();
        this.application = model.getApplication ();
    }


    /** {@inheritDoc} */
    @Override
    public void executeNormal (final ButtonEvent event)
    {
        // The transport buttons of the MPD32 do not reliably send a release event, therefore the
        // action is triggered on press
        if (event != ButtonEvent.DOWN || !this.application.isMixerLayout ())
            return;

        // If a clip is recording, stop the take instead of toggling the overdub
        final Optional<ISlot> recordingSlot = this.findRecordingSlot ();
        if (recordingSlot.isPresent ())
        {
            recordingSlot.get ().launch (true, false);
            return;
        }

        this.transport.toggleLauncherOverdub ();
    }


    /** {@inheritDoc} */
    @Override
    public void executeShifted (final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN || !this.application.isMixerLayout ())
            return;

        final Optional<ITrack> selectedTrack = this.model.getTrackBank ().getSelectedItem ();
        if (selectedTrack.isEmpty ())
            return;

        final ITrack track = selectedTrack.get ();
        final ISlotBank slotBank = track.getSlotBank ();
        final Optional<ISlot> selectedSlot = slotBank.getSelectedItem ();

        if (selectedSlot.isPresent () && selectedSlot.get ().isRecording ())
        {
            // Stop the take, the clip continues to play
            selectedSlot.get ().launch (true, false);
            return;
        }

        final int slotIndex = selectedSlot.map (ISlot::getIndex).orElse (0);
        track.recordNewLauncherClip (slotIndex);
    }


    /**
     * Find a slot which is recording on the selected track.
     *
     * @return The recording slot, empty if none is recording
     */
    private Optional<ISlot> findRecordingSlot ()
    {
        final Optional<ITrack> selectedTrack = this.model.getTrackBank ().getSelectedItem ();
        if (selectedTrack.isEmpty ())
            return Optional.empty ();

        final ISlotBank slotBank = selectedTrack.get ().getSlotBank ();
        for (int i = 0; i < slotBank.getPageSize (); i++)
        {
            final ISlot slot = slotBank.getItem (i);
            if (slot.isRecording () || slot.isRecordingQueued ())
                return Optional.of (slot);
        }

        return Optional.empty ();
    }
}
