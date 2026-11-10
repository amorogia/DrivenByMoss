// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2026
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.akai.mpd32;

import java.util.List;
import java.util.UUID;

import de.mossgrabers.framework.controller.DefaultControllerDefinition;
import de.mossgrabers.framework.utils.OperatingSystem;
import de.mossgrabers.framework.utils.Pair;


/**
 * Definition class for the Akai MPD32 controller extension.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class MPD32ControllerDefinition extends DefaultControllerDefinition
{
    private static final String PORT_NAME    = "Akai MPD32 Port 1";
    private static final UUID   EXTENSION_ID = UUID.fromString ("4716CA4E-D505-41BC-8AB6-8A5D8CB718D3");


    /**
     * Constructor.
     */
    public MPD32ControllerDefinition ()
    {
        super (EXTENSION_ID, "MPD32", "Akai", 1, 1);
    }


    /** {@inheritDoc} */
    @Override
    public List<Pair<String [], String []>> getMidiDiscoveryPairs (final OperatingSystem os)
    {
        // The device exposes 3 ports (Akai MPD32 Port 1/2/3), port 1 is used
        final List<Pair<String [], String []>> midiDiscoveryPairs = super.getMidiDiscoveryPairs (os);
        switch (os)
        {
            default:
            case WINDOWS:
            case MAC, MAC_ARM:
                midiDiscoveryPairs.add (this.addDeviceDiscoveryPair (PORT_NAME, PORT_NAME));
                break;

            case LINUX:
                midiDiscoveryPairs.addAll (this.createLinuxDeviceDiscoveryPairs (PORT_NAME, PORT_NAME));
                break;
        }
        return midiDiscoveryPairs;
    }
}
