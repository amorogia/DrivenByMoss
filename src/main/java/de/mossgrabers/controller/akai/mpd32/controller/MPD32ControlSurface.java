// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2026
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.akai.mpd32.controller;

import de.mossgrabers.controller.akai.mpd32.MPD32Configuration;
import de.mossgrabers.framework.controller.AbstractControlSurface;
import de.mossgrabers.framework.controller.color.ColorManager;
import de.mossgrabers.framework.daw.IHost;
import de.mossgrabers.framework.daw.midi.IMidiInput;


/**
 * The Akai MPD32 surface.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class MPD32ControlSurface extends AbstractControlSurface<MPD32Configuration>
{
    /** The number of knobs. */
    public static final int NUM_KNOBS = 8;

    /** The CC of the 1st knob. The other knobs follow consecutively (CC 0-7). */
    public static final int KNOB_1_CC = 0;

    /** The number of rotary knobs. */
    public static final int NUM_ROTARY_KNOBS = 8;

    /** The CC of the 1st rotary knob. The other knobs follow consecutively (CC 16-23). */
    public static final int ROTARY_KNOB_1_CC = 16;

    /** The number of buttons. */
    public static final int NUM_BUTTONS = 8;

    /** The CC of the 1st button. The other buttons follow consecutively (CC 8-15). */
    public static final int BUTTON_1_CC = 8;


    /**
     * Constructor.
     *
     * @param host The host
     * @param colorManager The color manager
     * @param configuration The configuration
     * @param input The MIDI input
     */
    public MPD32ControlSurface (final IHost host, final ColorManager colorManager, final MPD32Configuration configuration, final IMidiInput input)
    {
        super (0, host, configuration, colorManager, null, input, null, null, 800, 300);
    }
}
