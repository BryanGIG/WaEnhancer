package com.wmods.wppenhacer.xposed.features.general;

import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.wmods.wppenhacer.xposed.core.Feature;
import com.wmods.wppenhacer.xposed.core.components.FMessageWpp;
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator;
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class MenuStatus extends Feature {

    public static HashSet<MenuItemStatus> menuStatuses = new HashSet<>();

    public MenuStatus(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {

        var mediaClass = Unobfuscator.loadStatusDownloadMediaClass(classLoader);
        logDebug("Media class: " + mediaClass.getName());
//        var menuStatusClass = Unobfuscator.loadMenuStatusClass(classLoader);
        var menuStatusMethod = Unobfuscator.loadMenuStatusMethod(classLoader);
        logDebug("MenuStatus method: " + menuStatusMethod.getName());
        var fieldFile = Unobfuscator.loadStatusDownloadFileField(classLoader);
        logDebug("File field: " + fieldFile.getName());
        var clazzSubMenu = Unobfuscator.loadStatusDownloadSubMenuClass(classLoader);
        logDebug("SubMenu class: " + clazzSubMenu.getName());
        var clazzMenu = Unobfuscator.loadStatusDownloadMenuClass(classLoader);
        logDebug("Menu class: " + clazzMenu.getName());
        var menuField = ReflectionUtils.getFieldByType(clazzSubMenu, clazzMenu);
        logDebug("Menu field: " + menuField.getName());

        Class<?> StatusPlaybackBaseFragmentClass = classLoader.loadClass("com.whatsapp.status.playback.fragment.StatusPlaybackBaseFragment");
        Class<?> StatusPlaybackContactFragmentClass = classLoader.loadClass("com.whatsapp.status.playback.fragment.StatusPlaybackContactFragment");
        var listStatusField = ReflectionUtils.getFieldsByExtendType(StatusPlaybackContactFragmentClass, List.class).get(0);

        XposedBridge.hookMethod(menuStatusMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object fragmentInstance;
                Menu menu;
                if (param.args[0] instanceof Menu) {
                    menu = (Menu) param.args[0];
                    fragmentInstance = param.thisObject;
                } else {
                    var clazz = param.thisObject.getClass();
                    Field subMenuField = ReflectionUtils.findFieldUsingFilter(clazz, f -> f.getType() == Object.class && clazzSubMenu.isInstance(ReflectionUtils.getField(f, param.thisObject)));
                    Object subMenu = ReflectionUtils.getField(subMenuField, param.thisObject);
                    menu = (Menu) ReflectionUtils.getField(menuField, subMenu);
                    var fragment = ReflectionUtils.findFieldUsingFilter(clazz, f -> StatusPlaybackBaseFragmentClass.isInstance(ReflectionUtils.getField(f, param.thisObject)));
                    if (fragment == null) {
                        logDebug("Fragment not found");
                        return;
                    }
                    fragmentInstance = fragment.get(param.thisObject);
                }

                var index = (int) XposedHelpers.getObjectField(fragmentInstance, "A00");
                var listStatus = (List) listStatusField.get(fragmentInstance);
                var fMessage = new FMessageWpp(listStatus.get(index));

                for (MenuItemStatus menuStatus : menuStatuses) {
                    var menuItem = menuStatus.addMenu(menu, fMessage);
                    if (menuItem == null) continue;
                    menuItem.setOnMenuItemClickListener(item -> {
                        menuStatus.onClick(item, fragmentInstance, fMessage);
                        return true;
                    });
                }
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Menu Status";
    }

    public abstract static class MenuItemStatus {

        public abstract MenuItem addMenu(Menu menu, FMessageWpp fMessage);

        public abstract void onClick(MenuItem item, Object fragmentInstance, FMessageWpp fMessageWpp);
    }
}
