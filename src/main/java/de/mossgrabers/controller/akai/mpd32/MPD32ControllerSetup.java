// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2026
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.akai.mpd32;

import de.mossgrabers.controller.akai.mpd32.command.trigger.MPD32PlayCommand;
import de.mossgrabers.controller.akai.mpd32.command.trigger.MPD32RecordCommand;
import de.mossgrabers.controller.akai.mpd32.command.trigger.MPD32StopCommand;
import de.mossgrabers.controller.akai.mpd32.controller.MPD32ControlSurface;
import de.mossgrabers.framework.command.core.ContinuousCommand;
import de.mossgrabers.framework.command.core.NopCommand;
import de.mossgrabers.framework.configuration.ISettingsUI;
import de.mossgrabers.framework.controller.AbstractControllerSetup;
import de.mossgrabers.framework.controller.ButtonID;
import de.mossgrabers.framework.controller.ContinuousID;
import de.mossgrabers.framework.controller.ISetupFactory;
import de.mossgrabers.framework.controller.color.ColorEx;
import de.mossgrabers.framework.controller.color.ColorManager;
import de.mossgrabers.framework.controller.hardware.BindType;
import de.mossgrabers.framework.controller.hardware.IHwAbsoluteKnob;
import de.mossgrabers.framework.controller.valuechanger.TwosComplementValueChanger;
import de.mossgrabers.framework.daw.IHost;
import de.mossgrabers.framework.daw.ModelSetup;
import de.mossgrabers.framework.daw.constants.Resolution;
import de.mossgrabers.framework.daw.data.bank.IDrumPadBank;
import de.mossgrabers.framework.daw.data.bank.IParameterBank;
import de.mossgrabers.framework.daw.midi.IMidiAccess;
import de.mossgrabers.framework.daw.midi.IMidiInput;
import de.mossgrabers.framework.parameter.IParameter;
import de.mossgrabers.framework.utils.ButtonEvent;


/**
 * Setup to support the Akai MPD32 controller. The 8 knobs control the volume, panorama and sends of
 * the selected track, the 8 rotary knobs control the parameters of the selected device, the 8
 * buttons set the note repeat rate (buttons 1-7) and act as Shift (button 8). Pad bank A is mapped
 * to C1-D#2. The left/right buttons transpose it by octaves or, if the selected device supports drum
 * pads, page through its drum pads (the next page starts at E2).
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class MPD32ControllerSetup extends AbstractControllerSetup<MPD32ControlSurface, MPD32Configuration>
{
    /** The MIDI channel the knobs send on (channel 16, zero-based). */
    private static final int MIDI_CHANNEL = 15;


    /**
     * Constructor.
     *
     * @param host The DAW host
     * @param factory The factory
     * @param globalSettings The global settings
     * @param documentSettings The document (project) specific settings
     */
    public MPD32ControllerSetup (final IHost host, final ISetupFactory factory, final ISettingsUI globalSettings, final ISettingsUI documentSettings)
    {
        super (factory, host, globalSettings, documentSettings);

        this.valueChanger = new TwosComplementValueChanger (128, 1);

        // No color control at all
        this.colorManager = new ColorManager ();
        this.colorManager.registerColorIndex (ColorManager.BUTTON_STATE_OFF, 0);
        this.colorManager.registerColorIndex (ColorManager.BUTTON_STATE_ON, 0);
        this.colorManager.registerColorIndex (ColorManager.BUTTON_STATE_HI, 0);
        this.colorManager.registerColor (0, ColorEx.BLACK);

        this.configuration = new MPD32Configuration (host, this.valueChanger, factory.getArpeggiatorModes ());
    }


    /** {@inheritDoc} */
    @Override
    protected void createScales ()
    {
        // Not used
    }


    /** {@inheritDoc} */
    @Override
    protected void createModel ()
    {
        final ModelSetup ms = new ModelSetup ();
        ms.setHasFullFlatTrackList (true);
        ms.setNumTracks (8);
        ms.setNumSends (5);
        ms.setNumParams (MPD32ControlSurface.NUM_KNOBS);
        ms.setNumParamPages (8);
        ms.setNumDrumPadLayers (MPD32ControlSurface.NUM_PADS);

        this.model = this.factory.createModel (this.configuration, this.colorManager, this.valueChanger, this.scales, ms);
    }


    /** {@inheritDoc} */
    @Override
    protected void createSurface ()
    {
        final IMidiAccess midiAccess = this.factory.createMidiAccess ();
        // The pads are used as note input, the knobs and buttons are bound via CC
        final IMidiInput input = midiAccess.createInput ("MPD32 Pads", "8?????" /* Note off */, "9?????" /* Note on */);

        final MPD32ControlSurface surface = new MPD32ControlSurface (this.host, this.colorManager, this.configuration, input);
        this.surfaces.add (surface);

        // Map pad bank A (notes 48-63) to C1-D#2 (notes 36-51), all other notes are unchanged
        surface.updatePadNoteMapping ();
    }


    /** {@inheritDoc} */
    @Override
    protected void createObservers ()
    {
        super.createObservers ();

        this.configuration.registerDeactivatedItemsHandler (this.model);
        this.createNoteRepeatObservers (this.configuration, this.getSurface ());

        // Switch the pad mapping when the selected device changes
        this.model.getCursorDevice ().addHasDrumPadsObserver (hasDrumPads -> this.host.scheduleTask (this::updatePadMapping, 100));
        this.updatePadMapping ();
    }


    /**
     * Update the pad mapping depending on if the selected device supports drum pads or not.
     */
    private void updatePadMapping ()
    {
        final MPD32ControlSurface surface = this.getSurface ();
        final boolean drumMode = this.model.getCursorDevice ().hasDrumPads ();
        surface.setDrumMode (drumMode);
        if (drumMode)
            this.scrollDrumPads (surface.getDrumOffset ());
        else
            this.model.getCursorDevice ().getDrumPadBank ().setIndication (false);
    }


    /**
     * Scroll the drum pad bank so that its window matches the notes played by the MPD32. Note that
     * Bitwig does not scroll the chain view of the Drum Machine when the window of a drum pad bank is
     * changed, the indication only highlights the pads which are part of the window. To make the
     * chain view follow, enable "Follow Played Notes" in the Drum Machine (the five-pin MIDI icon),
     * the view then scrolls as soon as a pad of the new window is played.
     *
     * @param note The first note to display
     */
    private void scrollDrumPads (final int note)
    {
        final IDrumPadBank drumPadBank = this.model.getCursorDevice ().getDrumPadBank ();
        drumPadBank.setIndication (true);
        drumPadBank.scrollTo (note);
    }


    /** {@inheritDoc} */
    @Override
    protected void registerTriggerCommands ()
    {
        // The buttons send 127 when pressed and 0 when released.
        // Buttons 1-7 set the note repeat rate, button 8 is the Shift button
        final Resolution [] noteRepeatRates =
        {
            Resolution.RES_1_4,
            Resolution.RES_1_4T,
            Resolution.RES_1_8,
            Resolution.RES_1_8T,
            Resolution.RES_1_16,
            Resolution.RES_1_16T,
            Resolution.RES_1_32
        };
        for (int i = 0; i < noteRepeatRates.length; i++)
        {
            final Resolution rate = noteRepeatRates[i];
            final ButtonID buttonID = ButtonID.get (ButtonID.TRACK_SELECT_1, i);
            this.addButton (buttonID, rate.getName (), (event, velocity) -> this.configuration.setNoteRepeatPeriod (rate), MIDI_CHANNEL, MPD32ControlSurface.BUTTON_1_CC + i);
        }
        this.addButton (ButtonID.SHIFT, "Shift", NopCommand.INSTANCE, MIDI_CHANNEL, MPD32ControlSurface.BUTTON_1_CC + MPD32ControlSurface.NUM_BUTTONS - 1);
        // The stop button toggles note repeat, with Shift it switches the selected device off
        // Note: the transport buttons send on a different MIDI channel than the other controls
        this.addButton (ButtonID.REPEAT, "Repeat", new MPD32StopCommand (this.model, this.getSurface ()), MPD32ControlSurface.TRANSPORT_CHANNEL, MPD32ControlSurface.STOP_CC);
        // The play button toggles the playback, with Shift it continues from the stop position
        this.addButton (ButtonID.PLAY, "Play", new MPD32PlayCommand (this.model, this.getSurface ()), MPD32ControlSurface.TRANSPORT_CHANNEL, MPD32ControlSurface.PLAY_CC);
        // The record button toggles the clip launcher overdub, with Shift it records a clip in the
        // selected slot (Mix view only)
        this.addButton (ButtonID.RECORD, "Record", new MPD32RecordCommand (this.model, this.getSurface ()), MPD32ControlSurface.TRANSPORT_CHANNEL, MPD32ControlSurface.RECORD_CC);

        // The left/right buttons shift the octave of pad bank A. If a drum machine is selected they
        // page through the drum pads instead. With Shift they select the previous/next device of the
        // selected track.
        this.addButton (ButtonID.OCTAVE_DOWN, "Octave Down", (event, velocity) -> {
            if (event == ButtonEvent.DOWN)
                this.handleLeftRightButton (-1);
        }, MPD32ControlSurface.TRANSPORT_CHANNEL, MPD32ControlSurface.OCTAVE_DOWN_CC);
        this.addButton (ButtonID.OCTAVE_UP, "Octave Up", (event, velocity) -> {
            if (event == ButtonEvent.DOWN)
                this.handleLeftRightButton (1);
        }, MPD32ControlSurface.TRANSPORT_CHANNEL, MPD32ControlSurface.OCTAVE_UP_CC);
    }


    /**
     * Handle the left/right buttons. If Shift is pressed, the previous/next device of the selected
     * track is selected. Otherwise the pads are shifted by an octave or, if the selected device
     * supports drum pads, paged through its drum pads.
     *
     * @param direction -1 to go down/previous, 1 to go up/next
     */
    private void handleLeftRightButton (final int direction)
    {
        final MPD32ControlSurface surface = this.getSurface ();

        if (surface.isShiftPressed ())
        {
            if (direction < 0)
                this.model.getCursorDevice ().selectPrevious ();
            else
                this.model.getCursorDevice ().selectNext ();
            return;
        }

        final boolean hasDrumPads = this.model.getCursorDevice ().hasDrumPads ();

        if (hasDrumPads)
        {
            surface.setDrumMode (true);
            surface.shiftDrumOffset (direction * MPD32ControlSurface.NUM_PADS);
            this.scrollDrumPads (surface.getDrumOffset ());
            return;
        }

        surface.setDrumMode (false);
        surface.shiftPadOctave (direction);
    }


    /** {@inheritDoc} */
    @Override
    protected void registerContinuousCommands ()
    {
        // There are no modes, therefore everything is hard wired: slider 1 controls the volume
        // and slider 2 the panorama of the selected track, sliders 3-7 the send levels 1-5 of the
        // selected track, slider 8 the master volume when Shift is pressed
        final IParameterBank parameterBank = this.model.getCursorDevice ().getParameterBank ();

        for (int i = 0; i < MPD32ControlSurface.NUM_KNOBS; i++)
        {
            final ContinuousID continuousID = ContinuousID.get (ContinuousID.KNOB1, i);
            final boolean isLast = i == MPD32ControlSurface.NUM_KNOBS - 1;
            final ContinuousCommand command = isLast ? this::handleLastSlider : null;
            final IHwAbsoluteKnob knob = this.addAbsoluteKnob (continuousID, "Knob " + (i + 1), command, BindType.CC, MIDI_CHANNEL, MPD32ControlSurface.KNOB_1_CC + i);
            knob.setIndexInGroup (i);
            if (i == 0)
                knob.bind (this.model.getCursorTrack ().getVolumeParameter ());
            else if (i == 1)
                knob.bind (this.model.getCursorTrack ().getPanParameter ());
            else if (i >= 2 && i <= 6)
                knob.bind (this.model.getCursorTrack ().getSendBank ().getItem (i - 2));
        }

        // The rotary knobs control the parameters 1-8 of the selected device
        for (int i = 0; i < MPD32ControlSurface.NUM_ROTARY_KNOBS; i++)
        {
            final ContinuousID continuousID = ContinuousID.get (ContinuousID.DEVICE_KNOB1, i);
            final IHwAbsoluteKnob knob = this.addAbsoluteKnob (continuousID, "Rotary Knob " + (i + 1), null, BindType.CC, MIDI_CHANNEL, MPD32ControlSurface.ROTARY_KNOB_1_CC + i);
            knob.setIndexInGroup (i);
            knob.bind (parameterBank.getItem (i));
        }
    }


    /**
     * Handles the last slider: controls the master volume if Shift is pressed, otherwise ignored.
     *
     * @param value The new value of the slider
     */
    private void handleLastSlider (final int value)
    {
        if (this.getSurface ().isShiftPressed ())
            this.model.getMasterTrack ().getVolumeParameter ().setValue (this.valueChanger, value);
    }


    /** {@inheritDoc} */
    @Override
    public void startup ()
    {
        // Intentionally empty
    }


    /** {@inheritDoc} */
    @Override
    protected void layoutControls ()
    {
        final MPD32ControlSurface surface = this.getSurface ();

        final double padding = 10;
        final double width = 40;
        for (int i = 0; i < MPD32ControlSurface.NUM_KNOBS; i++)
            surface.getContinuous (ContinuousID.get (ContinuousID.KNOB1, i)).setBounds (padding + (padding + width) * i, 10, width, width);
        for (int i = 0; i < MPD32ControlSurface.NUM_BUTTONS - 1; i++)
            surface.getButton (ButtonID.get (ButtonID.TRACK_SELECT_1, i)).setBounds (padding + (padding + width) * i, 60, width, 30);
        for (int i = 0; i < MPD32ControlSurface.NUM_ROTARY_KNOBS; i++)
            surface.getContinuous (ContinuousID.get (ContinuousID.DEVICE_KNOB1, i)).setBounds (padding + (padding + width) * i, 110, width, width);
        surface.getButton (ButtonID.SHIFT).setBounds (padding + (padding + width) * (MPD32ControlSurface.NUM_BUTTONS - 1), 60, width, 30);
    }
}
