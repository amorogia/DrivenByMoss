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

    /** The CC of the stop button. */
    public static final int STOP_CC = 117;

    /** The CC of the play button. */
    public static final int PLAY_CC = 118;

    /** The CC of the record button. */
    public static final int RECORD_CC = 119;

    /** The MIDI channel the transport buttons (e.g. stop) send on (channel 1, zero-based). */
    public static final int TRANSPORT_CHANNEL = 0;

    /** The CC of the left (octave down) button. */
    public static final int OCTAVE_DOWN_CC = 115;

    /** The CC of the right (octave up) button. */
    public static final int OCTAVE_UP_CC = 116;

    /** The first note sent by pad bank A. */
    public static final int PAD_BANK_A_FIRST_NOTE = 48;

    /** The number of pads. */
    public static final int NUM_PADS = 16;

    /** The offset to map pad bank A to C1. */
    public static final int PAD_BANK_A_NOTE_OFFSET = -12;

    private int               padOctaveOffset;
    private int               drumOffset = PAD_BANK_A_FIRST_NOTE + PAD_BANK_A_NOTE_OFFSET;
    private boolean           drumMode;


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


    /**
     * Set if the pads should control the drum pads of a drum machine.
     *
     * @param drumMode True to control the drum pads
     */
    public void setDrumMode (final boolean drumMode)
    {
        if (this.drumMode == drumMode)
            return;

        this.drumMode = drumMode;
        this.updatePadNoteMapping ();
    }


    /**
     * Get the current first note of the drum pad window.
     *
     * @return The first note
     */
    public int getDrumOffset ()
    {
        return this.drumOffset;
    }


    /**
     * Shift the note mapping of pad bank A by octaves. Only used if not in drum mode.
     *
     * @param octaves The number of octaves to shift (e.g. -1 or 1)
     */
    public void shiftPadOctave (final int octaves)
    {
        final int newOffset = this.padOctaveOffset + octaves;
        final int firstNote = PAD_BANK_A_FIRST_NOTE + PAD_BANK_A_NOTE_OFFSET + newOffset * 12;
        if (firstNote < 0 || firstNote + NUM_PADS - 1 > 127)
            return;

        this.padOctaveOffset = newOffset;
        this.updatePadNoteMapping ();
    }


    /**
     * Shift the window of drum pads which is controlled by pad bank A.
     *
     * @param offset The offset in notes
     */
    public void shiftDrumOffset (final int offset)
    {
        final int newOffset = this.drumOffset + offset;
        if (newOffset < 0 || newOffset + NUM_PADS - 1 > 127)
            return;

        this.drumOffset = newOffset;
        this.updatePadNoteMapping ();
    }


    /**
     * Update the key translation table of the pads. Pad bank A is mapped to the configured octave or
     * drum pad window, all other notes are passed through unchanged.
     */
    public void updatePadNoteMapping ()
    {
        final int translation = this.drumMode ? this.drumOffset - PAD_BANK_A_FIRST_NOTE : PAD_BANK_A_NOTE_OFFSET + this.padOctaveOffset * 12;
        final int [] keyTranslationTable = new int [128];
        for (int i = 0; i < keyTranslationTable.length; i++)
        {
            final boolean isPadBankA = i >= PAD_BANK_A_FIRST_NOTE && i < PAD_BANK_A_FIRST_NOTE + NUM_PADS;
            keyTranslationTable[i] = isPadBankA ? i + translation : i;
        }
        this.setKeyTranslationTable (keyTranslationTable);
    }
}
