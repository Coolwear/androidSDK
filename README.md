2024/11/11 更新
1.增加hrv  --> com/coolwear/bluesdk/BluetoothManager.java

//接收手动，自动HRV数据    压力是HRV数据通过算法计算
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HRV_FOR_SHOW,
(K6BleDataResult<ArrayList<K6_HrvStruct>>) k6_heartStructs -> {
hrvLiveData.postValue(k6_heartStructs);
return false;
});


2.增加手势  -->com/coolwear/bluesdk/BluetoothManager.java
//接收设备手势开关状态
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_GESTURE_DATA,
(K6BleDataResult<K6_DATA_TYPE_GESTURE_CONFIG>) gesture_config -> {
GESTURE_OFF = gesture_config.getControlType();
return false;
});

3.增加GSensor    -->com/coolwear/bluesdk/BluetoothManager.java
//体感游戏XYZ轴信息 先要打开xyz轴开关 不用时关闭
//xyz轴开关
//BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(CEBC.OPENSTATUS.CLOSE);
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_MOTION_DATA,
(K6BleDataResult<k6_DATA_TYPE_MOTION_DATA>) motion_data -> {
int X = motion_data.getX_Move_Distance();
int Y = motion_data.getY_Move_Distance();
int Z = motion_data.getSpeed_Throw();
return false;
});


4.电量协议增加 充电状态 -->com/coolwear/bluesdk/BluetoothManager.java

k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_BATTERY, new K6BleDataResult<K6_DATA_TYPE_BATTERY_INFO>() {
@Override
public boolean bleDataResult(K6_DATA_TYPE_BATTERY_INFO battery_info) {
gBatteryLiveData.postValue(battery_info);
if (dataCallBack != null) {
dataCallBack.updateBattery(battery_info.getBattery());
}
return false;
}
});

5.增加温度  -->com/coolwear/bluesdk/BluetoothManager.java
//接收体温数据
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_TEMP_FOR_SHOW,
(K6BleDataResult<ArrayList<K6_TempStruct>>) tempStructs -> {
tempLiveData.postValue(tempStructs);
return false;
});


11/26

1.增加自动检测开关 timeInterval:以分钟为单位
BluetoothHelper.getInstance().getSendBlueData().sendHeartAutoSwitch(autoHeart,autoO2,timeInterval);
2.增加清除数据协议
BluetoothHelper.getInstance().getSendBlueData().sendClearData();


2025/09/15
/**
* 打开关闭摇摇拍照
* @param  true 开 false关
*/
BluetoothHelper.getInstance().getSendBlueData().sendPhotoSwitch(true);

//接收拍照指令
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_TAKE_PHOTOS, new K6BleDataResult<Integer>() {
@Override
public boolean bleDataResult(Integer integer) {
if (integer == CEBC.OPENSTATUS.OPEN) {
Log.i(TAG, "open/Take photo");
} else {
Log.i(TAG, "close");
}
return false;
}
});

2025/09/15
//手势控制
activityMainBinding.swHid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
   public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
      BluetoothHelper.getInstance().getSendBlueData().sendGestureConfig(b ? 
                                        K6_DATA_TYPE_GESTURE_CONFIG.GESTURE_CTRL_TYPE_TIKTOK
                                      : K6_DATA_TYPE_GESTURE_CONFIG.GESTURE_CTRL_TYPE_NONE);
         if (b) {
                    BluetoothHelper.getInstance().getSendBlueData().createBond(CEBlueSharedPreference.getDevAddress());
          } else {
                    BluetoothHelper.getInstance().getSendBlueData().removeBond(CEBlueSharedPreference.getDevAddress());

}
}
});

/**
* 接收设备手势开关状态 0：表示设备hid为关闭状态 大于0表示设备hid为开启状态
*/
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_GESTURE_DATA,
(K6BleDataResult<K6_DATA_TYPE_GESTURE_CONFIG>) gesture_config -> {
int GESTURE_OFF = gesture_config.getControlType();
hidLiveData.postValue(GESTURE_OFF);
return false;
});



修改记录
1. v1.2.8  同步ota传输速度  20260402
2. v1.2.9  增加心率PPG采集  20260618
3. v1.2.10  增加GensorPPG采集  20260624

## Release 签名打包

Release 签名从项目根目录的 `keystore.properties` 读取。首次配置时，复制
`keystore.properties.example` 为 `keystore.properties`，填写密钥文件路径、密码和别名。
`storeFile` 相对项目根目录解析，Windows 路径请使用 `/`。

本机已生成的签名文件为 `keystore/sdk-demo-release.jks`，别名为 `sdk-demo-release`，
密码保存在本机的 `keystore.properties` 中。这两个文件均已加入 Git 忽略规则，请单独备份，
后续版本继续使用同一份签名密钥。其他开发机需还原这两个本地文件后再打 release 包。

在项目根目录执行：

```powershell
.\gradlew.bat :app:assembleRelease
```

签名 APK 输出到 `app/build/outputs/apk/release/app-release.apk`。
如需 AAB，执行 `.\gradlew.bat :app:bundleRelease`，
输出到 `app/build/outputs/bundle/release/app-release.aab`。
缺少签名配置或密钥文件时，release 构建会报错提示；debug 构建不需要该配置。
