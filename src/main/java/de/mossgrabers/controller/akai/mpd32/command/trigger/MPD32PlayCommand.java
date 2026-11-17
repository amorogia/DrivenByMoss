// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2026
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.akai.mpd32.command.trigger;

import de.mossgrabers.controller.akai.mpd32.MPD32Configuration;
import de.mossgrabers.controller.akai.mpd32.controller.MPD32ControlSurface;
import de.mossgrabers.framework.command.core.AbstractTriggerCommand;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.ITransport;
import de.mossgrabers.framework.utils.ButtonEvent;


/**
 * Command for the play button. Without Shift it toggles the playback, with Shift it continues the
 * playback from the stop position.
 *
 * @author Jürgen Moßgraber
 */
public class MPD32PlayCommand extends AbstractTriggerCommand<MPD32ControlSurface, MPD32Configuration>
{
    private final ITransport transport;


    /**
     * Constructor.
     *
     * @param model The model
     * @param surface The surface
     */
    public MPD32PlayCommand (final IModel model, final MPD32ControlSurface surface)
    {
        super (model, surface);
        this.transport = this.model.getTransport ();
    }


    /** {@inheritDoc} */
    @Override
    public void executeNormal (final ButtonEvent event)
    {
        // The transport buttons of the MPD32 do not reliably send a release event, therefore the
        // playback is toggled on press
        if (event != ButtonEvent.DOWN)
            return;

        if (this.transport.isPlaying ())
            this.transport.stop ();
        else
            this.transport.play ();
    }


    /** {@inheritDoc} */
    @Override
    public void executeShifted (final ButtonEvent event)
    {
        if (event == ButtonEvent.DOWN)
            this.transport.continuePlayback ();
    }
}
