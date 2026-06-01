package com.bmd.archive.plugins;

import com.bmd.archive.plugins.providers.*;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import pt.ua.dicoogle.sdk.PluginSet;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.mlprovider.MLProviderInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@PluginImplementation
public class MlProviderPluginSet implements PluginSet, PlatformCommunicatorInterface {

    public static DicooglePlatformInterface coreDicoogle;

    private final List<MLProviderInterface> mlProviders;

    private ConfigurationHolder settings;

    public MlProviderPluginSet() {
        CellPoseProvider cellPoseProvider = new CellPoseProvider();
        mlProviders = Arrays.asList(cellPoseProvider);
    }

    @Override
    public String getName() {
        return "mlprovider";
    }

    @Override
    public Collection<? extends MLProviderInterface> getMLPlugins() {
        return mlProviders;
    }

    @Override
    public void setSettings(ConfigurationHolder configurationHolder) {
        this.settings = configurationHolder;
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface dicooglePlatformInterface) {
        coreDicoogle = dicooglePlatformInterface;
    }

}
