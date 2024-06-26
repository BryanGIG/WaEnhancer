package com.wmods.wppenhacer.xposed.features.media;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wmods.wppenhacer.xposed.core.DesignUtils;
import com.wmods.wppenhacer.xposed.core.Feature;
import com.wmods.wppenhacer.xposed.core.ResId;
import com.wmods.wppenhacer.xposed.core.Unobfuscator;
import com.wmods.wppenhacer.xposed.core.Utils;
import com.wmods.wppenhacer.xposed.core.WppCore;
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class DownloadProfile extends Feature {

    public DownloadProfile(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        var loadProfileInfoField = Unobfuscator.loadProfileInfoField(classLoader);
        XposedHelpers.findAndHookMethod("com.whatsapp.profile.ViewProfilePhoto", classLoader, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var menu = (Menu) param.args[0];
                var item = menu.add(0, 0, 0, "Save");
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                var icon = DesignUtils.getDrawableByName("ic_action_download");
                if (icon != null) {
                    icon.setTint(Color.WHITE);
                    item.setIcon(icon);
                }
                item.setOnMenuItemClickListener(menuItem -> {
                    var subCls = param.thisObject.getClass().getSuperclass();
                    if (subCls == null) {
                        log(new Exception("SubClass is null"));
                        return true;
                    }
                    var field = Unobfuscator.getFieldByType(subCls, loadProfileInfoField.getDeclaringClass());
                    var jidObj = ReflectionUtils.getField(loadProfileInfoField, ReflectionUtils.getField(field, param.thisObject));
                    var jid = WppCore.stripJID(WppCore.getRawString(jidObj));
                    var file = WppCore.getContactPhotoFile(jid);
                    var destPath = Utils.getDestination(prefs, "Profile Photo");
                    var name = Utils.generateName(jidObj, "jpg");
                    var error = Utils.copyFile(file, new File(destPath, name));
                    if (TextUtils.isEmpty(error)) {
                        Toast.makeText(Utils.getApplication(), Utils.getApplication().getString(ResId.string.saved_to) + destPath, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(Utils.getApplication(), Utils.getApplication().getString(ResId.string.error_when_saving_try_again) + " " + error, Toast.LENGTH_LONG).show();
                    }
                    return true;
                });
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Download Profile Picture";
    }
}
