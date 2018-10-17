package moe.plushie.armourers_workshop.proxies;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import com.mojang.authlib.GameProfile;

import moe.plushie.armourers_workshop.ArmourersWorkshop;
import moe.plushie.armourers_workshop.common.addons.ModAddonManager;
import moe.plushie.armourers_workshop.common.blocks.BlockSkinnable.Seat;
import moe.plushie.armourers_workshop.common.blocks.ModBlocks;
import moe.plushie.armourers_workshop.common.capability.ModCapabilityManager;
import moe.plushie.armourers_workshop.common.config.ConfigHandler;
import moe.plushie.armourers_workshop.common.config.ConfigHandlerOverrides;
import moe.plushie.armourers_workshop.common.config.ConfigSynchronizeHandler;
import moe.plushie.armourers_workshop.common.crafting.CraftingManager;
import moe.plushie.armourers_workshop.common.items.ModItems;
import moe.plushie.armourers_workshop.common.lib.LibModInfo;
import moe.plushie.armourers_workshop.common.library.CommonLibraryManager;
import moe.plushie.armourers_workshop.common.library.ILibraryCallback;
import moe.plushie.armourers_workshop.common.library.ILibraryManager;
import moe.plushie.armourers_workshop.common.library.LibraryFile;
import moe.plushie.armourers_workshop.common.library.LibraryFileType;
import moe.plushie.armourers_workshop.common.library.global.permission.PermissionSystem;
import moe.plushie.armourers_workshop.common.network.GuiHandler;
import moe.plushie.armourers_workshop.common.network.PacketHandler;
import moe.plushie.armourers_workshop.common.network.messages.client.MessageClientGuiAdminPanel.AdminPanelCommand;
import moe.plushie.armourers_workshop.common.network.messages.client.MessageClientGuiSkinLibraryCommand.SkinLibraryCommand;
import moe.plushie.armourers_workshop.common.network.messages.server.MessageServerClientCommand.CommandType;
import moe.plushie.armourers_workshop.common.network.messages.server.MessageServerLibrarySendSkin.SendType;
import moe.plushie.armourers_workshop.common.skin.SkinExtractor;
import moe.plushie.armourers_workshop.common.skin.cache.CommonSkinCache;
import moe.plushie.armourers_workshop.common.skin.cubes.CubeRegistry;
import moe.plushie.armourers_workshop.common.skin.data.Skin;
import moe.plushie.armourers_workshop.common.skin.entity.EntitySkinHandler;
import moe.plushie.armourers_workshop.common.skin.type.SkinTypeRegistry;
import moe.plushie.armourers_workshop.utils.ModLogger;
import moe.plushie.armourers_workshop.utils.SkinIOUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

@Mod.EventBusSubscriber(modid = LibModInfo.ID)
public class CommonProxy implements ILibraryCallback {
    
    private static MinecraftServer server;
    private static ModItems modItems;
    private static ModBlocks modBlocks;
    public ILibraryManager libraryManager;
    private PermissionSystem permissionSystem;
    
    public void preInit(FMLPreInitializationEvent event) {
        File configDir = event.getSuggestedConfigurationFile().getParentFile();
        configDir = new File(configDir, LibModInfo.ID);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        ModAddonManager.preInit();
        ConfigHandler.init(new File(configDir, "common.cfg"));
        ConfigHandlerOverrides.init(new File(configDir, "overrides.cfg"));
        
        EntityRegistry.registerModEntity(new ResourceLocation(LibModInfo.ID, "seat"), Seat.class, "seat", 1, ArmourersWorkshop.instance, 10, 20, false);
        
        SkinIOUtils.makeLibraryDirectory();
        SkinExtractor.extractSkins();
        
        SkinTypeRegistry.init();
        CubeRegistry.init();
        
        modItems = new ModItems();
        modBlocks = new ModBlocks();
        
        EntitySkinHandler.init();
        
        ModCapabilityManager.register();
    }
    
    
    public void initLibraryManager() {
        libraryManager = new CommonLibraryManager();
    }
    
    public void initRenderers() {}
    
    public void init(FMLInitializationEvent event) {
        modBlocks.registerTileEntities();
        CraftingManager.init();
        new GuiHandler();
        new ConfigSynchronizeHandler();
        
        PacketHandler.init();
        
        permissionSystem = new PermissionSystem();
        ModAddonManager.init();
    }
    
    public void postInit(FMLPostInitializationEvent event) {
        ModAddonManager.postInit();
        libraryManager.reloadLibrary();
    }
    
    public PermissionSystem getPermissionSystem() {
        return permissionSystem;
    }
    
    public void registerKeyBindings() {
        
    }
    
    public void receivedCommandFromSever(CommandType command) {
        
    }
    
    public void receivedAdminPanelCommand(EntityPlayer player, AdminPanelCommand command) {
        switch (command) {
        case RECOVER_SKINS:
            SkinIOUtils.recoverSkins(player);
            break;
        case RELOAD_LIBRARY:
            ArmourersWorkshop.getProxy().getLibraryManager().reloadLibrary();
            break;
        case UPDATE_SKINS:
            SkinIOUtils.updateSkins(player);
            break;
        case RELOAD_CACHE:
            CommonSkinCache.INSTANCE.clearAll();
            break;
        }
    }
    
    public void receivedSkinFromLibrary(String fileName, String filePath, Skin skin, SendType sendType) {
        
    }

    public void skinLibraryCommand(EntityPlayerMP player, SkinLibraryCommand command, LibraryFile file, boolean publicList) {
        switch (command) {
        case DELETE:
            if (!publicList) {
                File dir = new File(SkinIOUtils.getSkinLibraryDirectory(), file.filePath);
                if (file.isDirectory()) {
                    dir = new File(dir, file.fileName + "/");
                } else {
                    dir = new File(dir, file.fileName + SkinIOUtils.SKIN_FILE_EXTENSION);
                }
                if (dir.isDirectory() == file.isDirectory()) {
                    if (!SkinIOUtils.isInLibraryDir(dir)) {
                        ModLogger.log(Level.WARN, String.format("Player '%s' tried to delete the file/folder '%s' that is outside the library directory.", player.getGameProfile().toString(), dir.getAbsolutePath()));
                        return;
                    }
                    if (dir.exists()) {
                        if (file.isDirectory()) {
                            try {
                                FileUtils.deleteDirectory(dir);
                                libraryManager.reloadLibrary();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            clearFiles.add(file);
                            ModLogger.log("deleting skin " + dir.getAbsolutePath());
                            dir.delete();
                            libraryManager.removeFileFromListType(file, LibraryFileType.SERVER_PRIVATE, player);
                            libraryManager.reloadLibrary(this);
                        }
                    }
                }
            } else {
                
                ModLogger.log("public delete");
            }
            break;
        case NEW_FOLDER:
            if (!publicList) {
                File dir = new File(SkinIOUtils.getSkinLibraryDirectory(), file.filePath);
                dir = new File(dir, file.fileName);
                if (!SkinIOUtils.isInLibraryDir(dir)) {
                    ModLogger.log(Level.WARN, String.format("Player '%s' tried to make the folder '%s' that is outside the library directory.", player.getGameProfile().toString(), dir.getAbsolutePath()));
                    return;
                }
                if (!dir.exists()) {
                    dir.mkdir();
                }
                //TODO don't reload the library just add the folder
                libraryManager.reloadLibrary();
                ModLogger.log(String.format("making folder call %s in %s", file.fileName, file.filePath));
                ModLogger.log("full path: " + dir.getAbsolutePath());
            } else {
                ModLogger.log("public new folder");
            }
            break;
        }
    }
    
    private ArrayList<LibraryFile> clearFiles = new ArrayList<LibraryFile>();

    @Override
    public void libraryReloaded(ILibraryManager libraryManager) {
        for (int i = 0; i < clearFiles.size(); i++) {
            CommonSkinCache.INSTANCE.clearFileNameIdLink(clearFiles.get(i));
        }
    }
    
    public boolean isLocalPlayer(String username) {
        return false;
    }
    
    public boolean haveFullLocalProfile() {
        return false;
    }
    
    public GameProfile getLocalGameProfile() {
        return null;
    }
    
    public ILibraryManager getLibraryManager() {
        return libraryManager;
    }
    
    @SubscribeEvent
    public void serverStart(FMLServerStartingEvent event) {
        server = event.getServer();
    }
    
    @SubscribeEvent
    public MinecraftServer getServer() {
        return server;
    }
}
