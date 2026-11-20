package com.example.sdkdemo.ui;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sdkdemo.BluetoothManager;
import com.example.sdkdemo.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;
import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.K6_sleepData;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_BATTERY_INFO;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HEART_AUTO_SWITCH;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HR_CONTROL;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_REAL_O2;
import ce.com.cenewbluesdk.entity.k6.K6_DevInfoStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HeartStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HrvStruct;
import ce.com.cenewbluesdk.entity.k6.K6_MixSportType;
import ce.com.cenewbluesdk.entity.k6.K6_SEND_APP_SPORT_STRUCT;
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
import ce.com.cenewbluesdk.entity.k6.K6_StressStruct;
import ce.com.cenewbluesdk.entity.k6.K6_TempStruct;
import ce.com.cenewbluesdk.entity.k6.k6_GSENSOR_DATA;
import ce.com.cenewbluesdk.entity.k6.k6_RRI_HRV_DATA;
import ce.com.cenewbluesdk.proxy.interfaces.K6BleDataResult;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.proxy.sdkhelper.SendBlueToothData;
import ce.com.cenewbluesdk.uitl.Logger;

/**
 * Author:null
 * Date:2026/8/19 9:13
 * Description:添加您的描述
 */
public class SdkDemoActivity extends AppCompatActivity {

    private final static String TGA = "SdkDemoActivity";
    private LinearLayout tabReceive;
    private LinearLayout tabSend;

    private TextView tvReceive;
    private TextView tvSend;
    private TextView tvLog;

    private View lineReceive;
    private View lineSend;

    private ScrollView layoutReceive;
    private RecyclerView rvSend;

    private final List<String> sendItems = new ArrayList<>();
    private SdkSendAdapter sendAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TGA, "--onCreate--");
        setContentView(R.layout.activity_sdk_demo);
        initView();
        initSendData();
        showReceivePage();
        //Listen for received data
        receiveData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TGA, "--onResume--");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.ui_sdk_title);
        tvReceive.setText(R.string.receive_data);
        tvSend.setText(R.string.ui_send_data);
        findViewById(R.id.iv_back).setContentDescription(getString(R.string.ui_back));
        updateSendItems();
        sendAdapter.notifyDataSetChanged();
    }

    private void initView() {

        tabReceive =
                findViewById(R.id.tab_receive);

        tabSend =
                findViewById(R.id.tab_send);

        tvReceive =
                findViewById(R.id.tv_receive);

        tvSend =
                findViewById(R.id.tv_send);

        tvLog =
                findViewById(R.id.tv_log);

        lineReceive =
                findViewById(R.id.line_receive);

        lineSend =
                findViewById(R.id.line_send);

        layoutReceive =
                findViewById(R.id.layout_receive);

        rvSend =
                findViewById(R.id.rv_send);

        findViewById(R.id.iv_back)
                .setOnClickListener(v -> finish());

        tabReceive.setOnClickListener(v ->
                showReceivePage()
        );

        tabSend.setOnClickListener(v ->
                showSendPage()
        );
    }

    private void receiveData() {

        //Connection status
        BluetoothManager.connectStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer == K6_Action.RCVD.BLUE_CONNECTED) {
                    appendLog(R.string.ui_log_connected, CEBlueSharedPreference.getDeviceName(),
                            CEBlueSharedPreference.getDevAddress());
//                    //Data transmission channel is open; by default, the device does not upload data.
//                    BluetoothHelper.getInstance().setEnableGsDataTrans(true);
                    //Synchronize information to the device: e.g., phone time zone and time
                    BluetoothHelper.getInstance().getSendDataManager().sendAsynInfo();
                } else if (integer == K6_Action.RCVD.BLUE_CONNECTING) {
                    appendLog(R.string.ui_log_connecting, CEBlueSharedPreference.getDeviceName());
                } else if (integer == K6_Action.RCVD.BLUE_DISCONNECT) {
                    appendLog(R.string.ui_log_disconnected, CEBlueSharedPreference.getDeviceName());
                }

            }
        });

        //Heart rate monitoring is enabled
        BluetoothManager.heartAutoSwitch.observe(this, new Observer<K6_DATA_TYPE_HEART_AUTO_SWITCH>() {
            @Override
            public void onChanged(K6_DATA_TYPE_HEART_AUTO_SWITCH result) {
                appendLog(R.string.ui_log_auto_monitor, result);
            }
        });

        //Battery Information
        BluetoothManager.batteryLiveData.observe(this, new Observer<K6_DATA_TYPE_BATTERY_INFO>() {
            @Override
            public void onChanged(K6_DATA_TYPE_BATTERY_INFO battery_info) {
                appendLog(R.string.ui_log_battery, battery_info.getBattery(),
                        getString(battery_info.isChargerStatus() ? R.string.ui_yes : R.string.ui_no));
            }
        });

        //devInfo
        BluetoothManager.devInfoStructLive.observe(this, new Observer<K6_DevInfoStruct>() {
            @Override
            public void onChanged(K6_DevInfoStruct devInfoStruct) {
                appendLog(R.string.ui_log_device_info, devInfoStruct);
            }
        });

        //Heart rate data
        BluetoothManager.heartRate.observe(this, new Observer<List<K6_HeartStruct>>() {
            @Override
            public void onChanged(List<K6_HeartStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_HeartStruct heartStruct : result) {
                    stringBuilder.append(heartStruct.toString());
                }
                appendLog(R.string.ui_log_heart_rate, stringBuilder);

            }
        });

        //Blood oxygen data
        BluetoothManager.bloodOxygenData.observe(this, new Observer<List<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public void onChanged(List<K6_DATA_TYPE_REAL_O2> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_DATA_TYPE_REAL_O2 struct : result) {
                    stringBuilder.append(struct.toString());
                }
                appendLog(R.string.ui_log_blood_oxygen, stringBuilder);

            }
        });


        //hrv data
        BluetoothManager.hrvLiveData.observe(this, new Observer<List<k6_RRI_HRV_DATA>>() {
            @Override
            public void onChanged(List<k6_RRI_HRV_DATA> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (k6_RRI_HRV_DATA struct : result) {
                    stringBuilder.append(struct.toString());
                }
                appendLog(R.string.ui_log_hrv, stringBuilder);
            }
        });

        //stress data
        BluetoothManager.stressLiveData.observe(this, new Observer<List<K6_StressStruct>>() {
            @Override
            public void onChanged(List<K6_StressStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_StressStruct struct : result) {
                    stringBuilder.append(struct.toString());
                }
                appendLog(R.string.ui_log_stress, stringBuilder);
            }
        });


        //Temperature data
        BluetoothManager.tempLiveData.observe(this, new Observer<List<K6_TempStruct>>() {
            @Override
            public void onChanged(List<K6_TempStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_TempStruct tempStruct : result) {
                    stringBuilder.append(tempStruct.toString());
                }
                appendLog(R.string.ui_log_temperature, stringBuilder);
            }
        });

        //Sleep data
        BluetoothManager.sleepData.observe(this, new Observer<K6_sleepData>() {
            @Override
            public void onChanged(K6_sleepData result) {
                appendLog(R.string.ui_log_sleep, result);
            }
        });

        //  exercise data
        BluetoothManager.sportData.observe(this, new Observer<List<K6_Sport>>() {
            @Override
            public void onChanged(List<K6_Sport> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_Sport sport : result) {
                    stringBuilder.append(sport.toString());
                }
                appendLog(R.string.ui_log_motion, stringBuilder);
            }
        });

        //OTA progress
        BluetoothManager.otaFileProgress.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                appendLog(R.string.ui_log_ota_progress, integer);

            }
        });

        //Release status detection
        BluetoothManager.disposalStatus.observe(this, new Observer<K6_DATA_TYPE_HR_CONTROL>() {
            @Override
            public void onChanged(K6_DATA_TYPE_HR_CONTROL control) {
                if (control.controlState == 0) {
                    appendLog(R.string.ui_log_wear_device);
                } else if (control.controlState == 1) {
                    appendLog(R.string.ui_log_measurement_complete);
                }
            }
        });

        //hr ppg
        BluetoothManager.heartPpgLiveData.observe(this, new Observer<List<Integer>>() {
            @Override
            public void onChanged(List<Integer> payload) {
                appendLog(R.string.ui_log_heart_ppg, payload);
            }
        });


        //gSoner
        BluetoothManager.gSonerDataLiveData.observe(this, new Observer<List<k6_GSENSOR_DATA>>() {
            @Override
            public void onChanged(List<k6_GSENSOR_DATA> payload) {
                appendLog(R.string.ui_log_gsensor, payload);
            }
        });
        //hid
        BluetoothManager.hidLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer payload) {
                //Receiving device gesture switch status: 0: indicates the device HID is off; greater than 0: indicates the device HID is on.
                appendLog(R.string.ui_log_hid,
                        getString(payload > 0 ? R.string.ui_enabled : R.string.ui_disabled));
            }
        });
        //take photo
        BluetoothManager.takeLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer payload) {
                //Receiving device gesture switch status: 0: indicates the device HID is off; greater than 0: indicates the device HID is on.
                appendLog(R.string.ui_log_camera,
                        getString(payload == CEBC.OPENSTATUS.OPEN ? R.string.ui_enabled : R.string.ui_disabled));
            }
        });
        //Left/Right-Hand Settings
        BluetoothManager.LRDisplayLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer payload) {
                //Receiving device gesture switch status: 0: indicates the device HID is off; greater than 0: indicates the device HID is on.
                appendLog(R.string.ui_log_handedness,
                        getString(payload == 0 ? R.string.ui_left_hand : R.string.ui_right_hand));
            }
        });
        //counter
        BluetoothManager.counterLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer payload) {
                //Receiving device gesture switch status: 0: indicates the device HID is off; greater than 0: indicates the device HID is on.
                appendLog(R.string.ui_log_counter, payload);
            }
        });
    }

    private void initSendData() {
        updateSendItems();

        sendAdapter =
                new SdkSendAdapter(sendItems);

        rvSend.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvSend.setAdapter(sendAdapter);

        sendAdapter.setOnItemClickListener(position -> {

            switch (position) {

                case 0:
                    testHeartRate();
                    break;

                case 1:
                    testSpo2();
                    break;
                case 2:
                    testHRV();
                    break;

                case 3:
                    testStress();
                    break;

                case 4:
                    setAutoMonitor();
                    break;

                case 5:
                    appSportStart();
                    break;

                case 6:
                    appSportPause();
                    break;

                case 7:
                    appSportResume();
                    break;

                case 8:
                    appSportStop();
                    break;

                case 9:
                    shakeCamera();
                    break;

                case 10:
                    shutdownDevice();
                    break;

                case 11:
                    startOta();
                    break;
                case 12:
                    startHrPPg();
                    break;
                case 13:
                    startGensor();
                    break;
                case 14:
                    createHid();
                    break;
                case 15:
                    closeHid();
                    break;
                case 16:
                    setLRDisplay();
                    break;
                case 17:
                    requestCounterSync();
                    break;
                case 18:
                    clearCounter();
                    break;
                case 19:
                    unbindDevice();
                    finish();
                    break;
            }
            showReceivePage();
        });
    }

    private void updateSendItems() {
        sendItems.clear();
        String[] actions = getResources().getStringArray(R.array.ui_sdk_actions);
        for (int i = 0; i < actions.length; i++) {
            sendItems.add(getString(R.string.ui_action_numbered, i + 1, actions[i]));
        }
    }

    private void showReceivePage() {

        layoutReceive.setVisibility(View.VISIBLE);
        rvSend.setVisibility(View.GONE);

        tvReceive.setTextColor(Color.WHITE);
        tvReceive.setTypeface(null, Typeface.BOLD);

        tvSend.setTextColor(Color.parseColor("#BDBDBD"));

        tvSend.setTypeface(null, Typeface.NORMAL);

        lineReceive.setVisibility(View.VISIBLE);
        lineSend.setVisibility(View.INVISIBLE);
    }

    private void showSendPage() {

        layoutReceive.setVisibility(View.GONE);
        rvSend.setVisibility(View.VISIBLE);

        tvSend.setTextColor(Color.WHITE);
        tvSend.setTypeface(null, Typeface.BOLD);

        tvReceive.setTextColor(Color.parseColor("#BDBDBD"));

        tvReceive.setTypeface(null, Typeface.NORMAL);

        lineReceive.setVisibility(View.INVISIBLE);
        lineSend.setVisibility(View.VISIBLE);
    }

    /**
     * Add receive logs
     */
    private void appendLog(@StringRes int messageRes, Object... args) {
        appendLog(getString(messageRes, args));
    }

    private void appendSendLog(@StringRes int actionRes) {
        appendLog(R.string.ui_log_send, getString(actionRes));
    }

    public void appendLog(String log) {
        runOnUiThread(() -> {
            String old = tvLog.getText().toString();
            String newLog =
                    old + "\n" + getCurrentTime() + "  " + log;
            tvLog.setText(newLog);
            layoutReceive.post(() ->
                    layoutReceive.fullScroll(View.FOCUS_DOWN)
            );
        });
    }

    private String getCurrentTime() {

        return new SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindDevice();
        Logger.d(TGA, "--onDestroy--");
    }

    /**
     * Unbinding device
     *
     */
    private void unbindDevice() {
        BluetoothHelper.getInstance().unbindDevice();
    }

    /**
     * disConnect device
     *
     */
    private void disConnect() {
        BluetoothHelper.getInstance().disConnect();
    }

    private void testHeartRate() {
        appendSendLog(R.string.ui_action_heart_rate);
        //Start test: CEBC.OPENSTATUS.OPEN Stop test: CEBC.OPENSTATUS.CLOSE
        BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.OPEN);
    }

    private void testSpo2() {
        appendSendLog(R.string.ui_action_blood_oxygen);
        //Start test: CEBC.OPENSTATUS.OPEN Stop test: CEBC.OPENSTATUS.CLOSE
        BluetoothHelper.getInstance().getSendBlueData().sendBloodOxygenDetection(CEBC.OPENSTATUS.OPEN);
    }

    private void testStress() {
        appendSendLog(R.string.ui_action_stress);
        //Start test: CEBC.OPENSTATUS.OPEN Stop test: CEBC.OPENSTATUS.CLOSE
        BluetoothHelper.getInstance().getSendBlueData().sendStressSwitch(CEBC.OPENSTATUS.OPEN);
    }

    private void testHRV() {
        appendSendLog(R.string.ui_action_hrv);
        //Start test: CEBC.OPENSTATUS.OPEN Stop test: CEBC.OPENSTATUS.CLOSE
        BluetoothHelper.getInstance().getSendBlueData().sendRriHrvCmd(CEBC.OPENSTATUS.OPEN);
    }

    private void setAutoMonitor() {
        appendSendLog(R.string.ui_action_auto_monitor);
        //parameter1 Heart rate switch; parameter2 Blood oxygen switch; parameter3 Rate interval (minutes)
        BluetoothHelper.getInstance().getSendBlueData().sendHeartAutoSwitch(CEBC.OPENSTATUS.OPEN, CEBC.OPENSTATUS.OPEN, 5);
    }

    private void appSportStart() {
        appendSendLog(R.string.ui_action_sport_start);
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_START, 0, 0);
    }

    private void appSportPause() {
        appendSendLog(R.string.ui_action_sport_pause);
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_PAUSE, 0, 0);
    }

    private void appSportResume() {
        appendSendLog(R.string.ui_action_sport_resume);
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_CONTINUE, 0, 0);
    }

    private void appSportStop() {
        appendSendLog(R.string.ui_action_sport_stop);
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_STOP, 0, 0);
    }

    private void shakeCamera() {
        appendSendLog(R.string.ui_action_shake_camera);
        //true 开 ；false 关
        BluetoothHelper.getInstance().getSendBlueData().sendPhotoSwitch(true);

    }

    private void shutdownDevice() {
        appendSendLog(R.string.ui_action_shutdown);
        BluetoothHelper.getInstance().getSendBlueData().sendShutDown();
    }

    private void startOta() {
        appendSendLog(R.string.ui_action_ota);
        // localPath: The OTA package path can be downloaded from the server to a local file.
        //   BluetoothHelper.getInstance().getTransfer().otaUpgrade(localPath, "", "");
    }

    private void startHrPPg() {
        appendSendLog(R.string.ui_action_heart_ppg);
        //true: On; false: Off
        BluetoothHelper.getInstance().getSendBlueData().sendOffOnPpg(true);
    }

    private void startGensor() {
        appendSendLog(R.string.ui_action_gsensor);
        //true: On; false: Off
        BluetoothHelper.getInstance().getSendBlueData().sendOffOnGensor(true);
    }

    private void createHid() {
        appendSendLog(R.string.ui_action_hid_enable);
        SendBlueToothData.createBond(CEBlueSharedPreference.getDevAddress());
        BluetoothHelper.getInstance().getSendDataManager().sendGestureConfig(1);

    }

    private void closeHid() {
        appendSendLog(R.string.ui_action_hid_disable);
        SendBlueToothData.removeBond(CEBlueSharedPreference.getDevAddress());
        BluetoothHelper.getInstance().getSendDataManager().sendGestureConfig(0);
    }

    /// 0: Left hand; 1: Right hand
    private void setLRDisplay() {
        appendSendLog(R.string.ui_action_handedness);
        BluetoothHelper.getInstance().getSendDataManager().setLRDisplay(0);
    }

    private void requestCounterSync() {
        appendSendLog(R.string.ui_action_counter_get);
        BluetoothHelper.getInstance().getSendBlueData().requestCounterSync();
    }

    private void clearCounter() {
        appendSendLog(R.string.ui_action_counter_reset);
        BluetoothHelper.getInstance().getSendBlueData().clearCounter();
    }
}
