// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2026
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.bitwig.controller.akai.mpd32;

import de.mossgrabers.bitwig.framework.BitwigSetupFactory;
import de.mossgrabers.bitwig.framework.configuration.SettingsUIImpl;
import de.mossgrabers.bitwig.framework.daw.HostImpl;
import de.mossgrabers.bitwig.framework.extension.AbstractControllerExtensionDefinition;
import de.mossgrabers.controller.akai.mpd32.MPD32Configuration;
import de.mossgrabers.controller.akai.mpd32.MPD32ControllerDefinition;
import de.mossgrabers.controller.akai.mpd32.MPD32ControllerSetup;
import de.mossgrabers.controller.akai.mpd32.controller.MPD32ControlSurface;
import de.mossgrabers.framework.controller.IControllerSetup;

import com.bitwig.extension.controller.api.ControllerHost;


/**
 * Definition class for the Akai MPD32 controller.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class MPD32ControllerExtensionDefinition extends AbstractControllerExtensionDefinition<MPD32ControlSurface, MPD32Configuration>
{
    /**
     * Constructor.
     */
    public MPD32ControllerExtensionDefinition ()
    {
        super (new MPD32ControllerDefinition ());
    }


    /** {@inheritDoc} */
    @Override
    public boolean isUsingBetaAPI ()
    {
        return false;
    }


    /** {@inheritDoc} */
    @Override
    protected IControllerSetup<MPD32ControlSurface, MPD32Configuration> getControllerSetup (final ControllerHost host)
    {
        return new MPD32ControllerSetup (new HostImpl (host), new BitwigSetupFactory (host), new SettingsUIImpl (host, host.getPreferences ()), new SettingsUIImpl (host, host.getDocumentState ()));
    }
}
