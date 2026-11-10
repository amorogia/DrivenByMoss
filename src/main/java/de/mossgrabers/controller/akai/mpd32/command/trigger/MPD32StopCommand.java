// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2026
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.akai.mpd32.command.trigger;

import de.mossgrabers.controller.akai.mpd32.MPD32Configuration;
import de.mossgrabers.controller.akai.mpd32.controller.MPD32ControlSurface;
import de.mossgrabers.framework.command.core.AbstractTriggerCommand;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.utils.ButtonEvent;


/**
 * Command for the stop button. Without Shift it toggles note repeat, with Shift it toggles the
 * enabled state of the selected device.
 *
 * @author Jürgen Moßgraber
 */
public class MPD32StopCommand extends AbstractTriggerCommand<MPD32ControlSurface, MPD32Configuration>
{
    /**
     * Constructor.
     *
     * @param model The model
     * @param surface The surface
     */
    public MPD32StopCommand (final IModel model, final MPD32ControlSurface surface)
    {
        super (model, surface);
    }


    /** {@inheritDoc} */
    @Override
    public void executeNormal (final ButtonEvent event)
    {
        // The transport buttons of the MPD32 do not reliably send a release event, therefore the
        // note repeat is toggled on press
        if (event == ButtonEvent.DOWN)
        {
            this.surface.getConfiguration ().toggleNoteRepeatActive ();
            this.surface.getHost ().println ("MPD32: Note Repeat " + (this.surface.getConfiguration ().isNoteRepeatActive () ? "On" : "Off"));
        }
    }


    /** {@inheritDoc} */
    @Override
    public void executeShifted (final ButtonEvent event)
    {
        if (event == ButtonEvent.DOWN)
            this.model.getCursorDevice ().toggleEnabledState ();
    }
}
