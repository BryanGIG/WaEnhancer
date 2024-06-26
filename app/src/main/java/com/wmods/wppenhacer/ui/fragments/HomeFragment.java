package com.wmods.wppenhacer.ui.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.wmods.wppenhacer.App;
import com.wmods.wppenhacer.BuildConfig;
import com.wmods.wppenhacer.FilePicker;
import com.wmods.wppenhacer.MainActivity;
import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.databinding.FragmentHomeBinding;
import com.wmods.wppenhacer.ui.fragments.base.BaseFragment;
import com.wmods.wppenhacer.xposed.core.MainFeatures;
import com.wmods.wppenhacer.xposed.core.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

import rikka.core.util.IOUtils;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var intentFilter = new IntentFilter(BuildConfig.APPLICATION_ID + ".RECEIVER_WPP");
        ContextCompat.registerReceiver(requireContext(), new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    if (MainFeatures.PACKAGE_WPP.equals(intent.getStringExtra("PKG")))
                        receiverBroadcastWpp(context, intent);
                    else
                        receiverBroadcastBusiness(context, intent);
                } catch (Exception ignored) {
                }
            }
        }, intentFilter, ContextCompat.RECEIVER_EXPORTED);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        checkStateWpp(requireActivity());

        binding.rebootBtn.setOnClickListener(view -> {
            App.getInstance().restartApp(MainFeatures.PACKAGE_WPP);
            disableWpp(requireActivity());
        });

        binding.rebootBtn2.setOnClickListener(view -> {
            App.getInstance().restartApp(MainFeatures.PACKAGE_BUSINESS);
            disableBusiness(requireActivity());
        });

        binding.exportBtn.setOnClickListener(view -> saveConfigs(this.getContext()));
        binding.importBtn.setOnClickListener(view -> importConfigs(this.getContext()));
        binding.resetBtn.setOnClickListener(view -> resetConfigs(this.getContext()));

        return binding.getRoot();
    }

    @SuppressLint("StringFormatInvalid")
    private void receiverBroadcastBusiness(Context context, Intent intent) {
        binding.statusTitle3.setText(R.string.business_in_background);
        var version = intent.getStringExtra("VERSION");
        var supported_list = Arrays.asList(context.getResources().getStringArray(R.array.supported_versions_business));
        if (supported_list.contains(version)) {
            binding.statusSummary3.setText(getString(R.string.version_s, version));
            binding.status3.setCardBackgroundColor(context.getColor(rikka.material.R.color.material_green_500));
        } else {
            binding.statusSummary3.setText(getString(R.string.version_s_not_listed, version));
            binding.status3.setCardBackgroundColor(context.getColor(rikka.material.R.color.material_yellow_500));
        }
        binding.rebootBtn2.setVisibility(View.VISIBLE);
        binding.statusSummary3.setVisibility(View.VISIBLE);
        binding.statusIcon3.setImageResource(R.drawable.ic_round_check_circle_24);
    }

    @SuppressLint("StringFormatInvalid")
    private void receiverBroadcastWpp(Context context, Intent intent) {
        binding.statusTitle2.setText(R.string.whatsapp_in_background);
        var version = intent.getStringExtra("VERSION");
        var supported_list = Arrays.asList(context.getResources().getStringArray(R.array.supported_versions_wpp));

        if (supported_list.contains(version)) {
            binding.statusSummary1.setText(getString(R.string.version_s, version));
            binding.status2.setCardBackgroundColor(context.getColor(rikka.material.R.color.material_green_500));
        } else {
            binding.statusSummary1.setText(getString(R.string.version_s_not_listed, version));
            binding.status2.setCardBackgroundColor(context.getColor(rikka.material.R.color.material_yellow_500));
        }
        binding.rebootBtn.setVisibility(View.VISIBLE);
        binding.statusSummary1.setVisibility(View.VISIBLE);
        binding.statusIcon2.setImageResource(R.drawable.ic_round_check_circle_24);
    }

    private void resetConfigs(Context context) {
        var prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.getAll().forEach((key, value) -> prefs.edit().remove(key).apply());
        App.getInstance().restartApp(MainFeatures.PACKAGE_WPP);
        App.getInstance().restartApp(MainFeatures.PACKAGE_BUSINESS);
        Utils.showToast(context.getString(R.string.configs_reset), Toast.LENGTH_SHORT);
    }

    private void saveConfigs(Context context) {
        FilePicker.setOnUriPickedListener((uri) -> {
            try {
                try (var output = context.getContentResolver().openOutputStream(uri)) {
                    var prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    var entries = prefs.getAll();
                    var JSOjsonObject = new JSONObject();
                    for (var entry : entries.entrySet()) {
                        var keyValue = entry.getValue();
                        if (keyValue instanceof HashSet<?> hashSet) {
                            keyValue = new JSONArray(new ArrayList<>(hashSet));
                        }
                        JSOjsonObject.put(entry.getKey(), keyValue);
                    }
                    Objects.requireNonNull(output).write(JSOjsonObject.toString().getBytes());
                }
                Toast.makeText(context, context.getString(R.string.configs_saved), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        String formattedDate = dateFormat.format(new Date());
        FilePicker.fileSalve.launch("wpp_enhacer_configs_" + formattedDate + ".json");
    }

    private void importConfigs(Context context) {
        FilePicker.setOnUriPickedListener((uri) -> {
            try {
                try (var input = context.getContentResolver().openInputStream(uri)) {
                    var data = IOUtils.toString(input);
                    var prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    var jsonObject = new JSONObject(data);
                    prefs.getAll().forEach((key, value) -> prefs.edit().remove(key).apply());
                    var key = jsonObject.keys();
                    while (key.hasNext()) {
                        var keyName = key.next();
                        var value = jsonObject.get(keyName);
                        if (value instanceof JSONArray jsonArray) {
                            HashSet<String> hashSet = new HashSet<>();
                            for (var i = 0; i < jsonArray.length(); i++) {
                                hashSet.add(jsonArray.getString(i));
                            }
                            prefs.edit().putStringSet(keyName, hashSet).apply();
                        } else if (value instanceof String stringValue) {
                            prefs.edit().putString(keyName, stringValue).apply();
                        } else if (value instanceof Boolean booleanValue) {
                            prefs.edit().putBoolean(keyName, booleanValue).apply();
                        } else if (value instanceof Integer intValue) {
                            prefs.edit().putInt(keyName, intValue).apply();
                        } else if (value instanceof Long longValue) {
                            prefs.edit().putLong(keyName, longValue).apply();
                        } else if (value instanceof Float floatValue) {
                            prefs.edit().putFloat(keyName, floatValue).apply();
                        }
                    }
                }
                Toast.makeText(context, context.getString(R.string.configs_imported), Toast.LENGTH_SHORT).show();
                App.getInstance().restartApp(MainFeatures.PACKAGE_WPP);
                App.getInstance().restartApp(MainFeatures.PACKAGE_BUSINESS);
            } catch (Exception e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        FilePicker.fileCapture.launch(new String[]{"application/json"});
    }

    @SuppressLint("StringFormatInvalid")
    private void checkStateWpp(FragmentActivity activity) {

        if (MainActivity.isXposedEnabled()) {
            binding.statusIcon.setImageResource(R.drawable.ic_round_check_circle_24);
            binding.statusTitle.setText(R.string.module_enabled);
            binding.statusSummary.setText(String.format(getString(R.string.version_s), BuildConfig.VERSION_NAME));
        } else {
            binding.statusIcon.setImageResource(R.drawable.ic_round_error_outline_24);
            binding.statusTitle.setText(R.string.module_disabled);
            binding.status.setCardBackgroundColor(activity.getColor(rikka.material.R.color.material_red_500));
            binding.statusSummary.setVisibility(View.GONE);
        }
        if (isInstalled(MainFeatures.PACKAGE_WPP)) {
            disableWpp(activity);
        } else {
            binding.status2.setVisibility(View.GONE);
        }

        if (isInstalled(MainFeatures.PACKAGE_BUSINESS)) {
            disableBusiness(activity);
        } else {
            binding.status3.setVisibility(View.GONE);
        }
        checkWpp(activity);
        binding.deviceName.setText(Build.MANUFACTURER);
        binding.sdk.setText(String.valueOf(Build.VERSION.SDK_INT));
        binding.modelName.setText(Build.DEVICE);
        binding.listWpp.setText(Arrays.toString(activity.getResources().getStringArray(R.array.supported_versions_wpp)));
        binding.listBusiness.setText(Arrays.toString(activity.getResources().getStringArray(R.array.supported_versions_business)));
    }

    private boolean isInstalled(String packageWpp) {
        try {
            App.getInstance().getPackageManager().getPackageInfo(packageWpp, 0);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private void disableBusiness(FragmentActivity activity) {
        binding.statusIcon3.setImageResource(R.drawable.ic_round_error_outline_24);
        binding.statusTitle3.setText(R.string.business_is_not_running_or_has_not_been_activated_in_lsposed);
        binding.status3.setCardBackgroundColor(activity.getColor(rikka.material.R.color.material_red_500));
        binding.statusSummary3.setVisibility(View.GONE);
        binding.rebootBtn2.setVisibility(View.GONE);
    }

    private void disableWpp(FragmentActivity activity) {
        binding.statusIcon2.setImageResource(R.drawable.ic_round_error_outline_24);
        binding.statusTitle2.setText(R.string.whatsapp_is_not_running_or_has_not_been_activated_in_lsposed);
        binding.status2.setCardBackgroundColor(activity.getColor(rikka.material.R.color.material_red_500));
        binding.statusSummary1.setVisibility(View.GONE);
        binding.rebootBtn.setVisibility(View.GONE);
    }

    private static void checkWpp(FragmentActivity activity) {
        Intent checkWpp = new Intent(BuildConfig.APPLICATION_ID + ".CHECK_WPP");
        activity.sendBroadcast(checkWpp);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}