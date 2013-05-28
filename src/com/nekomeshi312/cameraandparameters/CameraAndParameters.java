package com.nekomeshi312.cameraandparameters;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.nekomeshi312.uitools.SeekBarPreference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.OrientationEventListener;

public class CameraAndParameters {
	public interface SetPreference{
		/**
		 * PreferenceActivityに表示する設定情報を設定する
		 * @param pref
		 * 親preference
		 * @param context
		 * 親のコンテキスト
		 * @return
		 */
		boolean setPreference(PreferenceCategory pref, Context context);
	}
	public abstract class CameraSettingBase{
		protected String mKey;
		protected String mDialogTitle;
		protected String mTitle;
		protected String mSummary;
		
		/**
		 * @return the mKey
		 */
		@SuppressWarnings("unused")
		public String getKey() {
			return mKey;
		}
		public abstract boolean isSupported();
		public abstract void setValueToCam();
		public abstract String printSettingCamValue();
		public abstract String printSettingPrefValue();
		
		
		protected abstract int getMinAPILevel();
		protected  String [] stringList2Array(List<String>list){
			int size = list.size();
			if(0 == size) return null;
	        String [] tmp = new String[size];
	        for(int i = 0;i < size;i++){
	        	tmp[i] = list.get(i);
	        }
	        return tmp;
		}
	}
	private abstract class CameraSettingString extends CameraSettingBase{
		protected boolean setPreferenceMain(PreferenceCategory pref, Context context){
			if(!isSupported())return false;
	        ListPreference listPref = new ListPreference(context);
	        listPref.setEntries(stringList2Array(getSupportedListName()));
	        listPref.setEntryValues(stringList2Array(getSupportedList()));
	        listPref.setDialogTitle(mDialogTitle);
	        listPref.setKey(mKey);
	        listPref.setTitle(mTitle);
	        listPref.setSummary(mSummary);
	        listPref.setDefaultValue(getValueFromCam());
	        pref.addPreference(listPref);
	        return true;
		}
		/**
		 * サポートされているかどうか
		 */
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			if(null == getSupportedList()) return false;
			return getSupportedList().size() > 0;
		}
		/**
		 * サポートされている項目の名称
		 * @return
		 */
		public abstract List<String> getSupportedList();
		/**
		 * サポートされている項目の設定画面に表示される名所
		 * @return
		 */
		public abstract List<String> getSupportedListName();
		
		/**
		 * カメラに設定されている値を取得する
		 * @return
		 */
		public abstract String getValueFromCam();

		/**
		 * Preferenceに登録されている値を取得する
		 * @return
		 */	
		@SuppressWarnings("unused")
		public String getValueFromPref(){
			String val = mSharedPref.getString(mKey, getValueFromCam());
			List<String> lst = getSupportedList();
			if(null == lst)return val;
			for(String s:lst){
				if(s.equals(val))return val;
			}
			return  lst.get(0);
		}
		
		@SuppressWarnings("unused")
		public String getValueName(){
			String cur = getValueFromPref();
			List<String> list = getSupportedList();
			for(int i = 0;i < list.size();i++){
				if(list.get(i).equals(cur)){
					return getSupportedListName().get(i);
				}
			}
			return null;
		}
		/**
		 * Preferenceに登録されている値を端末のカメラに設定する
		 */
		@SuppressWarnings("unused")
		public void setValueToCam(){
			setValueToCam(getValueFromPref());
		}
		/**
		 * 指定された値を端末のカメラに設定する
		 * @param value
		 */
		protected abstract void setValueToCam(String value);
		/**
		 * 指定された値をPreferenceに書き込む。また指定がある場合はカメラに反映させる
		 * @param value 設定する値
		 * @param setToCamNow trueの場合はカメラに反映させる
		 * @return true:書き込めた　false:書き込めなかった(対応していないなど）
		 */
		@SuppressWarnings("unused")
		public boolean setValueToPref(String value, boolean setToCamNow){
			String res = null;
			if(false != isSupported()){
				for(String s:getSupportedList()){
					if(s.equals(value)){
						res = s;
						break;
					}
				}
			}
		    Editor ed = mSharedPref.edit();
	    	ed.putString(mKey, res == null ? NOT_SUPPORTED_STRING:res);
			ed.commit();
			if(true == setToCamNow && null != res){
				setValueToCam(res);
			}
			return res != null;
		}
		@Override
		public String printSettingCamValue() {
			// TODO Auto-generated method stub
			String msg = isSupported()? getValueFromCam():mContext.getString(R.string.not_supported);
			return mKey + "(Cam)=" + msg;
		}
		@Override
		public String printSettingPrefValue() {
			// TODO Auto-generated method stub
			String msg = isSupported()? getValueFromPref():mContext.getString(R.string.not_supported);
			return mKey + "(Pref)=" + msg;
		}
	}
	//蛍光灯でのしましま防止
	public class CameraSettingAntibanding extends CameraSettingString
											implements SetPreference{
		private static final String LOG_TAG = "CameraSettingAntibanding";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingAntibanding() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mAntibanding));
			mDialogTitle = mContext.getString(R.string.anti_banding_dialog_title);
			mTitle = mContext.getString(R.string.anti_banding_title);
			mSummary = mContext.getString(R.string.anti_banding_summary);
		}

		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
	        return setPreferenceMain(pref, context);
		}

		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedAntibanding();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.ANTIBANDING_AUTO)){
					name = mContext.getString(R.string.anti_banding_auto);
				}
				else if(s.equals(Parameters.ANTIBANDING_50HZ)){
					name = mContext.getString(R.string.anti_banding_50Hz);
				}
				else if(s.equals(Parameters.ANTIBANDING_60HZ)){
					name = mContext.getString(R.string.anti_banding_60Hz);
				}
				else if(s.equals(Parameters.ANTIBANDING_OFF)){
					name = mContext.getString(R.string.anti_banding_off);			
				}
				else{
					name = mContext.getString(R.string.anti_banding_etc) + "(" + s + ")";					
				}
				ans.add(name);
			}
			return ans;
		}

		@Override
		public String getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getAntibanding();
		}

		@Override
		protected void setValueToCam(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setAntibanding(value);
			mCamera.setParameters(param);
		}
	}
	
	//カラーエフェクト
	public class CameraSettingColorEffect extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingColorEffect";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingColorEffect() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mColorEffect));
			mDialogTitle = mContext.getString(R.string.color_effect_dialog_title);
			mTitle = mContext.getString(R.string.color_effect_title);
			mSummary = mContext.getString(R.string.color_effect_summary);
		}

		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
	        return setPreferenceMain(pref, context);
		}
		
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getSupportedColorEffects();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.EFFECT_NONE)){
					name =  mContext.getString(R.string.color_effect_none);
				}
				else if(s.equals(Parameters.EFFECT_MONO)){
					name =  mContext.getString(R.string.color_effect_mono);
				}
				else if(s.equals(Parameters.EFFECT_NEGATIVE)){
					name =  mContext.getString(R.string.color_effect_negative);
				}
				else if(s.equals(Parameters.EFFECT_SOLARIZE)){
					name =  mContext.getString(R.string.color_effect_solarize);
				}
				else if(s.equals(Parameters.EFFECT_SEPIA)){
					name =  mContext.getString(R.string.color_effect_sepia);
				}
				else if(s.equals(Parameters.EFFECT_POSTERIZE)){
					name =  mContext.getString(R.string.color_effect_posterize);
				}
				else if(s.equals(Parameters.EFFECT_WHITEBOARD)){
					name =  mContext.getString(R.string.color_effect_whiteboard);
				}
				else if(s.equals(Parameters.EFFECT_BLACKBOARD)){
					name =  mContext.getString(R.string.color_effect_blackboard);
				}
				else if(s.equals(Parameters.EFFECT_AQUA)){
					name =  mContext.getString(R.string.color_effect_aqua);
				}
				else{
					name =  mContext.getString(R.string.color_effect_etc) + "(" + s + ")";
				}
				ans.add(name);
			}
			return ans;
		}
		@Override
		public String getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getColorEffect();
		}

		@Override
		protected void setValueToCam(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setColorEffect(value);
			mCamera.setParameters(param);
		}


	}
	
	//フラッシュモード
	public class CameraSettingFlashMode extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingFlashMode";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingFlashMode() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mFlashMode));
			mDialogTitle =  mContext.getString(R.string.flash_mode_dialog_title);
			mTitle =  mContext.getString(R.string.flash_mode_title);
			mSummary =  mContext.getString(R.string.flash_mode_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
	        return setPreferenceMain(pref, context);
		}
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedFlashModes();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.FLASH_MODE_OFF)){
					name =  mContext.getString(R.string.flash_mode_off);
				}
				else if(s.equals(Parameters.FLASH_MODE_AUTO)){
					name =  mContext.getString(R.string.flash_mode_auto);
				}
				else if(s.equals(Parameters.FLASH_MODE_ON)){
					name =  mContext.getString(R.string.flash_mode_on);
				}
				else if(s.equals(Parameters.FLASH_MODE_RED_EYE)){
					name =  mContext.getString(R.string.flash_mode_red_eye);
				}
				else if(s.equals(Parameters.FLASH_MODE_TORCH)){
					name = mContext.getString(R.string.flash_mode_torch);
				}
				else{
					name =  mContext.getString(R.string.flash_mode_etc) + "(" + s + ")";
				}
				ans.add(name);
			}
			return ans;
		}
		@Override
		public String getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getFlashMode();
		}
		
		@Override
		protected void setValueToCam(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setFlashMode(value);
			mCamera.setParameters(param);
		}
	}

	//フォーカスモード
	public class CameraSettingFocusMode extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingFocusMode";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingFocusMode() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mFocusMode));
			mDialogTitle =  mContext.getString(R.string.focus_mode_dialog_title);
			mTitle =  mContext.getString(R.string.focus_mode_title);
			mSummary =  mContext.getString(R.string.focus_mode_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			//ICSからAutoFocus or Macro 以外ではonAutoFocus()でtrueが帰らなくなった。要注意
			return param.getSupportedFocusModes();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				Log.w(LOG_TAG, s);
				if(s.equals(Parameters.FOCUS_MODE_AUTO)){
					name =  mContext.getString(R.string.focus_mode_auto);
				}
				else if(s.equals(Parameters.FOCUS_MODE_INFINITY)){
					name = mContext.getString(R.string.focus_mode_infinity);
				}
				else if(s.equals(Parameters.FOCUS_MODE_MACRO)){
					name =  mContext.getString(R.string.focus_mode_macro);
				}
				else if(s.equals(Parameters.FOCUS_MODE_FIXED)){
					name =  mContext.getString(R.string.focus_mode_fixed);
				}
				else{
					name =  mContext.getString(R.string.focus_mode_etc) + "(" + s + ")";
					try{
						Field f = Camera.Parameters.class.getDeclaredField("FOCUS_MODE_EDOF");
						if(s.equals((String)f.get(null))){
							name =  mContext.getString(R.string.focus_mode_EDOF);
						}
					}
					catch(Exception e){
						if(MyDebug.DEBUG)e.printStackTrace();
					}
					try{
						Field f = Camera.Parameters.class.getDeclaredField("FOCUS_MODE_CONTINUOUS_VIDEO");
						if(s.equals((String)f.get(null))){
							name =  mContext.getString(R.string.focus_mode_continuous_video);
						}
					}
					catch(Exception e){
						if(MyDebug.DEBUG)e.printStackTrace();
					}
					try{
						Field f = Camera.Parameters.class.getDeclaredField("FOCUS_MODE_CONTINUOUS_PICTURE");
						if(s.equals((String)f.get(null))){
							name =  mContext.getString(R.string.focus_mode_continuous_pic);
						}
					}
					catch(Exception e){
						if(MyDebug.DEBUG)e.printStackTrace();
					}
				}
				ans.add(name);
			}
			return ans;
		}
		@Override
		public String getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getFocusMode();
		}
	
		@Override
		protected void setValueToCam(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setFocusMode(value);
			mCamera.setParameters(param);
		}
	}

	
	//シーンモード
	public class CameraSettingSceneMode extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingSceneMode";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingSceneMode() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mSceneMode));
			mDialogTitle =  mContext.getString(R.string.scene_mode_dialog_title);
			mTitle = mContext.getString(R.string.scene_mode_title);
			mSummary = mContext.getString(R.string.scene_mode_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedSceneModes();
		}
		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.SCENE_MODE_AUTO)){
					name = mContext.getString(R.string.scene_mode_auto);
				}
				else if(s.equals(Parameters.SCENE_MODE_ACTION)){
					name = mContext.getString(R.string.scene_mode_action);
				}
				else if(s.equals(Parameters.SCENE_MODE_PORTRAIT)){
					name = mContext.getString(R.string.scene_mode_portrait);
				}
				else if(s.equals(Parameters.SCENE_MODE_LANDSCAPE)){
					name = mContext.getString(R.string.scene_mode_landscape);
				}
				else if(s.equals(Parameters.SCENE_MODE_NIGHT)){
					name = mContext.getString(R.string.scene_mode_night);
				}
				else if(s.equals(Parameters.SCENE_MODE_NIGHT_PORTRAIT)){
					name = mContext.getString(R.string.scene_mode_night_portrait);
				}
				else if(s.equals(Parameters.SCENE_MODE_THEATRE)){
					name = mContext.getString(R.string.scene_mode_theatre);
				}
				else if(s.equals(Parameters.SCENE_MODE_BEACH)){
					name = mContext.getString(R.string.scene_mode_beach);
				}
				else if(s.equals(Parameters.SCENE_MODE_SNOW)){
					name = mContext.getString(R.string.scene_mode_snow);
				}
				else if(s.equals(Parameters.SCENE_MODE_SUNSET)){
					name = mContext.getString(R.string.scene_mode_sunset);
				}
				else if(s.equals(Parameters.SCENE_MODE_STEADYPHOTO)){
					name = mContext.getString(R.string.scene_mode_steadyphoto);
				}
				else if(s.equals(Parameters.SCENE_MODE_FIREWORKS)){
					name = mContext.getString(R.string.scene_mode_fireworks);
				}
				else if(s.equals(Parameters.SCENE_MODE_SPORTS)){
					name = mContext.getString(R.string.scene_mode_sports);
				}
				else if(s.equals(Parameters.SCENE_MODE_PARTY)){
					name = mContext.getString(R.string.scene_mode_party);
				}
				else if(s.equals(Parameters.SCENE_MODE_CANDLELIGHT)){
					name = mContext.getString(R.string.scene_mode_candle_light);
				}
				else{
					name = mContext.getString(R.string.scene_mode_etc) + "(" + s + ")";
					try{
						Field f = Camera.Parameters.class.getDeclaredField("SCENE_MODE_BARCODE");
						if(s.equals((String)f.get(null))){
							name =  mContext.getString(R.string.scene_mode_barcode);
						}
					}
					catch(Exception e){
						if(MyDebug.DEBUG)e.printStackTrace();
					}
				}
				ans.add(name);
			}
			return ans;
		}

		@Override
		protected void setValueToCam(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setSceneMode(value);
			mCamera.setParameters(param);
		}
		@Override
		public String getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getSceneMode();
		}
	}

	
	//ホワイトバランス
	public class CameraSettingWhiteBalance extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingWhiteBalance";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingWhiteBalance() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mWhiteBalance));
			mDialogTitle = mContext.getString(R.string.white_balance_dialog_title);
			mTitle = mContext.getString(R.string.white_balance_title);
			mSummary = mContext.getString(R.string.white_balance_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedWhiteBalance();
		}
		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.WHITE_BALANCE_AUTO)){
					name = mContext.getString(R.string.white_balance_auto);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_INCANDESCENT)){
					name = mContext.getString(R.string.white_balance_incandesent);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_FLUORESCENT)){
					name = mContext.getString(R.string.white_balance_fluoresent);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_WARM_FLUORESCENT)){
					name = mContext.getString(R.string.white_balance_warm_fluorecent);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_DAYLIGHT)){
					name = mContext.getString(R.string.white_balance_daylight);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT)){
					name = mContext.getString(R.string.white_balance_cloudy_daylight);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_TWILIGHT)){
					name = mContext.getString(R.string.white_balance_twilight);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_SHADE)){
					name = mContext.getString(R.string.white_balance_shade);
				}
				else{
					name = mContext.getString(R.string.white_balance_etc) + "(" + s + ")";
				}
				ans.add(name);
			}
			return ans;
		}
		@Override
		protected void setValueToCam(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setWhiteBalance(value);
			mCamera.setParameters(param);
		}
		@Override
		public String getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getWhiteBalance();
		}
	}
	
	
	private abstract class CameraSettingCameraSize extends CameraSettingBase{	
		protected int correctValue(int i){
			i = Math.max(i, 0);
			i = Math.min(i, getSupportedList().size()-1);
			return i;
		}
		protected boolean setPreferenceMain(PreferenceCategory pref, Context context){
			if(!isSupported())return false;
	        ListPreference listPref = new ListPreference(context);
	        listPref.setEntries(stringList2Array(getSupportedListName()));
	        listPref.setEntryValues(stringList2Array(getSupportedListString()));
	        listPref.setDialogTitle(mDialogTitle);
	        listPref.setKey(mKey);
	        listPref.setTitle(mTitle);
	        listPref.setSummary(mSummary);
	        listPref.setDefaultValue(getValueNumFromCam());

	        pref.addPreference(listPref);
	        return true;
		}
		/**
		 * サポートされているかどうかを取得する
		 */
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			if(null == getSupportedList()) return false;
			return getSupportedList().size() > 0;
		}
		
		private static final String KEY_HEADER = "X";
		/**
		 * Preference Entry Value用にリストの番号を文字列に変換する。String.valueof()で単に数字を文字にするとうまくいかない
		 * 端末があるっぽいので、頭にXをつけておく
		 * @param number　文字列に変換する数字
		 * @return
		 */
		private String intToStringForPref(int number){
			return KEY_HEADER + number;
		}
		private int stringToIntForPref(String key){
			String s = key.substring(KEY_HEADER.length());
			int ans;
			try{
				ans = Integer.parseInt(s);
			}
			catch(Exception e){
				if(MyDebug.DEBUG)e.printStackTrace();
				ans = 0;//Prefに設定されている値が変だったときは0を入れておく
			}
			return ans;
		}
		/**
		 * サポートされている解像度一覧を取得する。特にソートはされていない
		 * @return
		 */
		public abstract List<Camera.Size> getSupportedList();
		/**
		 * サポートされている解像度一覧を文字列で取得する。
		 * @return
		 */
		public List<String> getSupportedListString(){
			ArrayList<String>tmp = new ArrayList<String>();
			for(int i = 0;i < getSupportedList().size();i++){
				tmp.add(intToStringForPref(i));
			}
			return tmp;
		}
		/**
		 * サポートされている解像度一覧を、設定メニューに表示する文字列として取得する
		 * @return
		 */
		public List<String> getSupportedListName(){
			ArrayList<String>tmp = new ArrayList<String>();
			for(Camera.Size s:getSupportedList()){
				String str = s.width + " x " + s.height;
				tmp.add(str);
			}
			return tmp;
		}
		/**
		 * カメラに設定されている値を取得する
		 * @return
		 */
		public abstract Camera.Size getValueFromCam();
		
		/**
		 * カメラに設定されているサイズが getSupportedList()で返されるリストの何番目かを取得する
		 * @return　リスト番号の文字列。(存在しない場合は"-1"となるが、カメラから取得したあたいを使っているので-1になることは無いはず
		 */
		private String getValueNumFromCam(){
			Camera.Size s = getValueFromCam();
			return intToStringForPref(getSupportedListNumber(s));
		}
		/**
		 * ListPreferenceは選択番号を文字列で管理しているため、その文字列を数字に直す
		 * @return
		 */
		private int getPrefValue(){
			String s = mSharedPref.getString(mKey, getValueNumFromCam());
			int ans = stringToIntForPref(s);
			ans = correctValue(ans);
			return ans;
		}
		public String getValueName(){
			return getSupportedListName().get(getPrefValue());
		}
		/**
		 * 現在Preferenceに設定されている解像度を取得する
		 * @return
		 */
		@SuppressWarnings("unused")
		public Camera.Size getValueFromPref(){
			return getSupportedList().get(getPrefValue());
		}
		/**
		 * 現在Preferenceに設定されている解像度を端末のカメラに反映させる
		 */
		@SuppressWarnings("unused")
		public void setValueToCam(){
			int prefVal = getPrefValue();
			if(MyDebug.DEBUG)Log.w(LOG_TAG, "prefVal = " + prefVal);
			setValueToCam(prefVal);
		}
		/**
		 * 指定された順番にあるカメラの解像度を端末のカメラに反映させる
		 * @param pos
		 */
		protected abstract void setValueToCam(int pos);
		/**
		 * @param size
		 * @param setNow
		 * @return
		 */
		/**
		 * 指定された解像度をPreferenceに書きこむ
		 * @param size 書きこむ解像度。指定された解像度をカメラがサポートしていない場合はデフォルトを書きこむ
		 * @param setToCamNow true:すぐに端末のカメラに設定する　false：させない　
		 * @return
		 */
		@SuppressWarnings("unused")
		public boolean setValueToPref(Camera.Size size, boolean setToCamNow){
			if(false == isSupported()){
				return false;
			}
			int i = getSupportedListNumber(size);
		    Editor ed = mSharedPref.edit();
		    //ListPreferenceは選択番号を文字列で管理しているため、文字列にする
		    String str = i < 0 ? getValueNumFromCam():String.valueOf(i);
	    	ed.putString(mKey, str);
			ed.commit();
			if(true == setToCamNow && i >= 0){
				setValueToCam(i);
			}
			return i >= 0;
		}
		private int getSupportedListNumber(Camera.Size s){
			List<Camera.Size>cList = getSupportedList();
			for(int i = 0;i < cList.size();i++){
				if(s.width == cList.get(i).width && s.height == cList.get(i).height){
					return i;
				}
			}
			return -1;
		}
		@Override
		public String printSettingCamValue() {
			// TODO Auto-generated method stub
			String msg;
			if(isSupported()){
				msg = String.valueOf(getValueFromCam().width)+"x"+String.valueOf(getValueFromCam().height);
			}
			else{
				msg = mContext.getString(R.string.not_supported);
			}
			return mKey + "(Cam)=" + msg;
		}
		@Override
		public String printSettingPrefValue() {
			// TODO Auto-generated method stub
			String msg;
			if(isSupported()){
				msg = String.valueOf(getValueFromPref().width)+"x"+String.valueOf(getValueFromPref().height);
			}
			else{
				msg = mContext.getString(R.string.not_supported);
			}
			return mKey + "(Pref)=" + msg;
		}
		
	}
	
	
	
	public class CameraSettingPictureSize extends CameraSettingCameraSize
											implements SetPreference{
		private static final String LOG_TAG = "CameraSettingPictureSize";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingPictureSize() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mPictureSize));
			mDialogTitle = mContext.getString(R.string.picture_size_dialog_title);
			mTitle = mContext.getString(R.string.picture_size_title);
			mSummary = mContext.getString(R.string.picture_size_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<Camera.Size> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedPictureSizes();
		}
		@Override
		protected void setValueToCam(int pos) {
			// TODO Auto-generated method stu
			List<Camera.Size> cm = getSupportedList();
			pos = correctValue(pos);
			int width = cm.get(pos).width;
			int height = cm.get(pos).height;
			Camera.Parameters param;
			param = mCamera.getParameters();
			param.setPictureSize(width, height);
			mCamera.setParameters(param);
		}
		@Override
		public Size getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getPictureSize();
		}

	}

	public class CameraSettingPreviewSize extends CameraSettingPictureSize{
		private static final String LOG_TAG = "CameraSettingPreviewSize";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingPreviewSize() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mPreviewSize));
			mDialogTitle = mContext.getString(R.string.preview_size_dialog_title);
			mTitle = mContext.getString(R.string.preview_size_title);
			mSummary = mContext.getString(R.string.preview_size_summary);
		}
		@Override
		public List<Camera.Size> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedPreviewSizes();
		}
		@Override
		protected void setValueToCam(int pos) {
			// TODO Auto-generated method stu
			List<Camera.Size> cm = getSupportedList();
			pos = correctValue(pos);
			int width = cm.get(pos).width;
			int height = cm.get(pos).height;
			Camera.Parameters param;
			param = mCamera.getParameters();
			param.setPreviewSize(width, height);
			mCamera.setParameters(param);
		}
		@Override
		public Size getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getPreviewSize();
		}
	}

	public class CameraSettingJpegThumbnailSize extends CameraSettingCameraSize
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingJpegThumbnailSize";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingJpegThumbnailSize() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mJpegThumbnailSize));
			mDialogTitle = mContext.getString(R.string.jpeg_thumbnail_size_dialog_title);
			mTitle = mContext.getString(R.string.jpeg_thumbnail_size_title);
			mSummary = mContext.getString(R.string.jpeg_thumbnail_size_summary);
		}
		@Override
		public List<Camera.Size> getSupportedList() {
			// TODO Auto-generated method stub
			Method method;
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				method = Camera.Parameters.class.
						getMethod("getSupportedJpegThumbnailSizes", new Class[] {});
				return (List<Camera.Size>) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return null;
			}
		}
		@Override
		protected void setValueToCam(int pos) {
			// TODO Auto-generated method stub
			if(isSupported() == false) return;
			
			List<Camera.Size> cm = getSupportedList();
			pos = correctValue(pos);

			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method;
				method = Camera.Parameters.class.
							getMethod("setJpegThumbnailSize", new Class[] {
																		int.class, 
																		int.class});
				method.invoke(param, cm.get(pos).width, cm.get(pos).height);
				mCamera.setParameters(param);
			} 
			catch (Exception e) {
				if(MyDebug.DEBUG)e.printStackTrace();
			}
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public Size getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getJpegThumbnailSize();
		}
	}
	public class CameraSettingVideoSize extends CameraSettingCameraSize
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingVideoSize";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 11;
		}
		public CameraSettingVideoSize() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mVideoSize));
			mDialogTitle = mContext.getString(R.string.video_size_dialog_title);
			mTitle = mContext.getString(R.string.video_size_title);
			mSummary = mContext.getString(R.string.video_size_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<Camera.Size> getSupportedList() {
			// TODO Auto-generated method stub
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method;
				method = Camera.Parameters.class.
									getMethod("getSupportedVideoSizes", new Class[] {});
				return (List<Camera.Size>) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return null;
			}	
		}
		
		public Camera.Size getPreferredPreviewSizeForVideo(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return null;
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();
				Method method;
				method = Camera.Parameters.class.
									getMethod("getPreferredPreviewSizeForVideo", new Class[] {});
				return (Camera.Size) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return null;
			}	

		}
		@Override
		protected void setValueToCam(int pos) {
			// TODO Auto-generated method stub
			//AndroidのカメラではVideoのサイズの指定はしない？MediaRecorderに設定する？
			//なのでここでは何もしない
			return;
		}
		@Override
		public Size getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			//AndroidのカメラではVideoのサイズの指定はしない？MediaRecorderに設定する？
			//なのでここではsupportedListで取れる一覧の先頭を選択する。
			return getSupportedList().get(0);
		}
	}
	
	private abstract class CameraSettingInteger extends CameraSettingBase{
		protected boolean setPreferenceMain(PreferenceCategory pref, Context context){
			if(!isSupported())return false;
	        ListPreference listPref = new ListPreference(context);
	        listPref.setEntries(stringList2Array(getSupportedListName()));
	        ArrayList<String>entryValues = new ArrayList<String>();
	        for(int i:getSupportedList()){
	        	entryValues.add(createValueKey(i));
	        }
	        listPref.setEntryValues(stringList2Array(entryValues));
	        listPref.setDialogTitle(mDialogTitle);
	        listPref.setKey(mKey);
	        listPref.setTitle(mTitle);
	        listPref.setSummary(mSummary);
	        listPref.setDefaultValue(createValueKey(getValueFromCam()));
	        pref.addPreference(listPref);
	        return true;
		}
		protected abstract String numbertoName(int no);


		/**
		 * サポートされているかどうか
		 */
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			if(null == getSupportedList()) return false;
			return getSupportedList().size() > 0;
		}
		/**
		 * サポートされている項目の名称
		 * @return
		 */
		public abstract List<Integer> getSupportedList();
		/**
		 * サポートされている項目の設定画面に表示される名所
		 * @return
		 */
		public abstract List<String> getSupportedListName();
		
		/**
		 * カメラに設定されている値を取得する
		 * @return
		 */
		public abstract int getValueFromCam();

		private static final String VAL = "val";
		private String createValueKey(int a){
			return VAL + String.valueOf(a);
		}
		private int createValueFromKey(String key){
			String[] tmp = key.split(VAL);
			int ans;
			try{
				ans = Integer.parseInt(tmp[1]);
			}
			catch(Exception e){
				e.printStackTrace();
				ans = 0;
			}
			return ans;
		}
		/**
		 * Preferenceに登録されている値を取得する
		 * @return
		 */	
		@SuppressWarnings("unused")
		public int getValueFromPref(){
			String v = mSharedPref.getString(mKey, createValueKey(getValueFromCam()));
			int val = createValueFromKey(v);
			List<Integer> lst = getSupportedList();
			if(null == lst)return val;
			for(int i:lst){
				if(i == val) return val;
			}
			return  lst.get(0);
		}
		
		@SuppressWarnings("unused")
		public String getValueName(){
			int cur = getValueFromPref();
			List<Integer> list = getSupportedList();
			for(int i = 0;i < list.size();i++){
				if(i == cur){
					return getSupportedListName().get(i);
				}
			}
			return null;
		}
		/**
		 * Preferenceに登録されている値を端末のカメラに設定する
		 */
		@SuppressWarnings("unused")
		public void setValueToCam(){
			setValueToCam(getValueFromPref());
		}
		/**
		 * 指定された値を端末のカメラに設定する
		 * @param value
		 */
		protected abstract void setValueToCam(int value);
		/**
		 * @param setNow　
		 * @return true:書き込めた　false:書き込めなかった(対応していないなど）
		 */
		/**
		 * 指定された値をPreferenceに書き込む。また指定がある場合はカメラに反映させる
		 * @param value 設定する値
		 * @param setToCamNow trueの場合はカメラに反映させる
		 * @return
		 */
		@SuppressWarnings("unused")
		public boolean setValueToPref(int value, boolean setToCamNow){
			Integer res = null;
			if(false != isSupported()){
				for(Integer i:getSupportedList()){
					if(i == value){
						res = i;
						break;
					}
				}
			}
		    Editor ed = mSharedPref.edit();
	    	ed.putString(mKey, res == null ? NOT_SUPPORTED_STRING:createValueKey(res));
			ed.commit();
			if(true == setToCamNow && null != res){
				setValueToCam(res);
			}
			return res != null;
		}
		@Override
		public String printSettingCamValue() {
			// TODO Auto-generated method stub
			String msg = isSupported()? numbertoName(getValueFromCam()):mContext.getString(R.string.not_supported);
			return mKey + "(Cam)=" + msg;
		}
		@Override
		public String printSettingPrefValue() {
			// TODO Auto-generated method stub
			String msg = isSupported()? numbertoName(getValueFromPref()):mContext.getString(R.string.not_supported);
			return mKey + "(Pref)=" + msg;
		}
	}

	public class CameraSettingPictureFormat extends CameraSettingInteger
										implements SetPreference{

		CameraSettingPictureFormat(){
			mKey = addCameraID(mContext.getString(R.string.mPictureFormat));
			mDialogTitle =  mContext.getString(R.string.picture_format_dialog_title);
			mTitle = mContext.getString(R.string.picture_format_title);
			mSummary = mContext.getString(R.string.picture_format_summary);
			
		}
		protected String numbertoName(int no){
			//ImageFormatクラスはAPI level8以降
			if(no == IMG_FORMAT_JPEG ){
				return mContext.getString(R.string.image_format_jpeg);
			}
			else if(no == IMG_FORMAT_NV16){
				return mContext.getString(R.string.image_format_nv16);
			}
			else if(no == IMG_FORMAT_NV21){
				return mContext.getString(R.string.image_foramt_nv21);
			}
			else if(no == IMG_FORMAT_RGB_565){
				return mContext.getString(R.string.image_format_rgb_565);
			}
			else if(no == IMG_FORMAT_UNKNOWN){
				return mContext.getString(R.string.image_format_nuknown);
			}
			else if(no == IMG_FORMAT_YUY2){
				return mContext.getString(R.string.image_format_yuy2);
			}
			else if(no == IMG_FORMAT_YV12){
				return mContext.getString(R.string.image_format_yu12);
			}
			else{
				switch(no){//API level 8 未満の場合
					case 0x00000100:
						return mContext.getString(R.string.image_format_jpeg);
					case 0x00000010:
						return mContext.getString(R.string.image_format_nv16);
					case 0x00000011:
						return mContext.getString(R.string.image_foramt_nv21);
					case 0x00000004:
						return mContext.getString(R.string.image_format_rgb_565);
					case 0x00000000:
						return mContext.getString(R.string.image_format_nuknown);
					case 0x00000014:
						return mContext.getString(R.string.image_format_yuy2);
					case 0x32315659:
						return mContext.getString(R.string.image_format_yu12);
					default:
						String fmt = "New Format(No = " + String.valueOf(no) + ")";
						return fmt;
				}
			}
		}

		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}

		@Override
		public List<Integer> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getSupportedPictureFormats ();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>names = new ArrayList<String>();
			List<Integer> list = getSupportedList();
			if(list == null) return null;
			for(int i = 0;i <list.size();i++){
				names.add(numbertoName(list.get(i)));
			}
			return names;
		}

		@Override
		public int getValueFromCam() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getPictureFormat();
		}

		@Override
		protected void setValueToCam(int value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			param.setPictureFormat(value);
			mCamera.setParameters(param);
		}

		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		} 
		
	}
	public class CameraSettingPreviewFormat extends CameraSettingPictureFormat{
		CameraSettingPreviewFormat(){
			mKey = addCameraID(mContext.getString(R.string.mPreviewFormat));
			mDialogTitle =  mContext.getString(R.string.preview_format_dialog_title);
			mTitle = mContext.getString(R.string.preview_format_title);
			mSummary = mContext.getString(R.string.preview_format_summary);
			
		}

		@Override
		public List<Integer> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getSupportedPreviewFormats ();
		}

		@Override
		public int getValueFromCam() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getPreviewFormat();
		}

		@Override
		protected void setValueToCam(int value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			param.setPreviewFormat(value);
			mCamera.setParameters(param);
		}

		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		} 
	}

	public class CameraSettingPreviewFrameRate extends CameraSettingInteger
													implements SetPreference{

		CameraSettingPreviewFrameRate(){
			mKey = addCameraID(mContext.getString(R.string.mPreviewFrameRate));
			mDialogTitle =  mContext.getString(R.string.preview_frame_rate_dialog_title);
			mTitle = mContext.getString(R.string.preview_frame_rate_title);
			mSummary = mContext.getString(R.string.preview_frame_rate_summary);
		}

		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}

		@Override
		public List<Integer> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getSupportedPreviewFrameRates();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>names = new ArrayList<String>();
			List<Integer> list = getSupportedList();
			if(list == null) return null;
			for(int i = 0;i <list.size();i++){
				names.add(numbertoName(list.get(i)));
			}
			return names;
		}

		@Override
		public int getValueFromCam() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getPreviewFrameRate();
		}

		@Override
		protected void setValueToCam(int value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			param.setPreviewFrameRate(value);
			mCamera.setParameters(param);
		}
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}

		@Override
		protected String numbertoName(int no) {
			// TODO Auto-generated method stub
			String name = String.valueOf(no) + "fps";
			return name;
		} 
	}
	
	
	private abstract class CameraSettingIntInRange extends CameraSettingBase{
		protected int	mRangeMin;
		protected int 	mRangeMax;
		
		protected int correctValue(int i){
			i = Math.max(i, getMinValue());
			i = Math.min(i, getMaxValue());
			return i;
		}
		protected int correctValue(float f){
			return correctValue((int)(f > 0f ? (f + 0.5f):(f - 0.5)));
		}
		/**
		 * 使用するレンジの最大値を取得する
		 * @return
		 */
		public abstract int getMaxValue();
		/**
		 * 使用するレンジの最小値を取得する
		 * @return
		 */
		public abstract int getMinValue();
		
		/**
		 * サポートされているかどうかを取得する
		 */
		public abstract boolean isSupported();
		public abstract int getValueFromCam();
		
		@SuppressWarnings("unused")
		public int getValueFromPref(){
			float ans = mSharedPref.getFloat(mKey, getValueFromCam());
			return correctValue(ans);
		}
		/**
		 * Preferenceに設定されている値を端末のカメラに反映させる
		 */
		@SuppressWarnings("unused")
		public void setValueToCam(){
			setValueToCam(getValueFromPref());
		}
		/**
		 * 指定された値を端末のカメラに反映させる
		 * @param zoom
		 */
		protected abstract  void setValueToCam(int zoom);
		/**
		 * @param i
		 * @param setNow
		 * 
		 * @return
		 * サポートしていない場合はfalse:
		 */
		/**
		 * 指定された値をPreferenceに書きこむ
		 * @param i 書きこむ値
		 * @param setToCamNow true:端末のカメラに反映させる　false:させない
		 * @return
		 */
		@SuppressWarnings("unused")
		public boolean setValueToPref(int i, boolean setToCamNow){
			if(false == isSupported()){
				return false;
			}
			i = correctValue(i);
						
		    Editor ed = mSharedPref.edit();
	    	ed.putFloat(mKey, i);//SeekBarがfloatで扱うため
			ed.commit();
			if(true == setToCamNow){
				setValueToCam(i);
			}
			return true;
		}
		@Override
		public String printSettingCamValue() {
			// TODO Auto-generated method stub
			String msg = isSupported() ? String.valueOf(getValueFromCam()):mContext.getString(R.string.not_supported);
			return mKey + "(Cam)=" + msg;
		}
		@Override
		public String printSettingPrefValue() {
			// TODO Auto-generated method stub
			String msg = isSupported() ? String.valueOf(getValueFromPref()):mContext.getString(R.string.not_supported);
			return mKey + "(Pref)=" + msg;
		}
	}
	public class CameraSettingJpegQuality extends CameraSettingIntInRange
										implements SetPreference{
		private static final int MIN_VAL = 50;
		private static final int MAX_VAL = 100;
		private static final int DEF_VAL = 90;

		private static final String LOG_TAG = "CameraSettingJpegQuality";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingJpegQuality() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mJpegQuality));
			mRangeMin = MIN_VAL;
			mRangeMax = MAX_VAL;
			mDialogTitle = mContext.getString(R.string.jpeg_quality_dialog_title);
			mTitle = mContext.getString(R.string.jpeg_quality_title);
			mSummary = mContext.getString(R.string.jpeg_quality_summary);
		}
		
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			if(!isSupported())return false;
			if(MyDebug.DEBUG) Log.i(LOG_TAG, mKey + " "+
					mRangeMin + " "+
					mRangeMax + " " +
					getValueFromCam());
	        SeekBarPreference seekBarPref = new SeekBarPreference(context);
	        seekBarPref.setDialogTitle(mDialogTitle);
	        seekBarPref.setKey(mKey);
	        seekBarPref.setTitle(mTitle);
	        seekBarPref.setSummary(mSummary);
	        seekBarPref.setRange(mRangeMin, mRangeMax, getValueFromCam());
	        pref.addPreference(seekBarPref);
	        return true;
		}
		@Override
		public boolean isSupported() {
			// TODO Auto-generated method stub
			return getMinAPILevel() <= android.os.Build.VERSION.SDK_INT;
		}
		@Override
		public int getMaxValue() {
			// TODO Auto-generated method stub
			return MAX_VAL;
		}
		@Override
		public int getMinValue() {
			// TODO Auto-generated method stub
			return MIN_VAL;
		}
		@Override
		protected void setValueToCam(int val) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			val = correctValue(val);
			param.setJpegQuality (val);
			mCamera.setParameters(param);
		}
		@Override
		public int getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return -1;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getJpegQuality();
		}
	}
	public class CameraSettingJpegThumbnailQuality extends CameraSettingJpegQuality{
		//getJpegThumbnailQuality () API5
		//ublic void setJpegThumbnailQuality (int quality) API5
		private static final int MIN_VAL = 50;
		private static final int MAX_VAL = 100;
		private static final int DEF_VAL = 90;

		private static final String LOG_TAG = "CameraSettingJpegQuality";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingJpegThumbnailQuality() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mJpegThumbnailQuality));
			mRangeMin = MIN_VAL;
			mRangeMax = MAX_VAL;
			mDialogTitle = mContext.getString(R.string.jpeg_thumbnail_quality_dialog_title);
			mTitle = mContext.getString(R.string.jpeg_thumbnail_quality_title);
			mSummary = mContext.getString(R.string.jpeg_thumbnail_quality_summary);
		}
		
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			if(!isSupported())return false;
			if(MyDebug.DEBUG) Log.i(LOG_TAG, mKey + " "+
					mRangeMin + " "+
					mRangeMax + " " +
					getValueFromCam());
	        SeekBarPreference seekBarPref = new SeekBarPreference(context);
	        seekBarPref.setDialogTitle(mDialogTitle);
	        seekBarPref.setKey(mKey);
	        seekBarPref.setTitle(mTitle);
	        seekBarPref.setSummary(mSummary);
	        seekBarPref.setRange(mRangeMin, mRangeMax, getValueFromCam());
	        pref.addPreference(seekBarPref);
	        return true;
		}
		@Override
		public boolean isSupported() {
			// TODO Auto-generated method stub
			return getMinAPILevel() <= android.os.Build.VERSION.SDK_INT;
		}
		@Override
		public int getMaxValue() {
			// TODO Auto-generated method stub
			return MAX_VAL;
		}
		@Override
		public int getMinValue() {
			// TODO Auto-generated method stub
			return MIN_VAL;
		}
		@Override
		protected void setValueToCam(int val) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			val = correctValue(val);
			param.setJpegThumbnailQuality (val);
			mCamera.setParameters(param);
		}
		@Override
		public int getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return -1;
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getJpegThumbnailQuality();
		}
		
	}
	public class CameraSettingZoom extends CameraSettingIntInRange
											implements SetPreference{
		private static final String LOG_TAG = "CameraSettingZoom";
		private static final int MIN_VAL = 0;
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 8;
		}
		public CameraSettingZoom() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mZoom));
			mRangeMin = getMinValue();
			mRangeMax = getMaxValue();
			mDialogTitle = mContext.getString(R.string.zoom_dialog_title);
			mTitle = mContext.getString(R.string.zoom_title);
			mSummary = mContext.getString(R.string.zoom_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			if(MyDebug.DEBUG) Log.i(LOG_TAG, mKey + " "+
					mRangeMin + " "+
					mRangeMax + " " +
					getValueFromCam());
			if(!isSupported())return false;

	        SeekBarPreference seekBarPref = new SeekBarPreference(context);
	        seekBarPref.setDialogTitle(mDialogTitle);
	        seekBarPref.setKey(mKey);
	        seekBarPref.setTitle(mTitle);
	        seekBarPref.setSummary(mSummary);
	        seekBarPref.setRange(mRangeMin, mRangeMax, getValueFromCam());
	        pref.addPreference(seekBarPref);
	        return true;
		}
		@Override
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"isZoomSupported", new Class[] {});
				Object o =  method.invoke(param, (Object [])null);
				return (boolean)Boolean.valueOf(o.toString());
			} 
			catch (Exception e) {
				return false;
			}
		}
		public boolean isSmoothZoomSupported(){
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"isSmoothZoomSupported", new Class[] {});
				Object o =  method.invoke(param, (Object [])null);
				return (boolean)Boolean.valueOf(o.toString());
			} 
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		@Override
		public int getMaxValue(){
			if(false == isSupported()){
				return NOT_SUPPORTED_INT;
			}
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"getMaxZoom", new Class[] {});
				Object o =  method.invoke(param, (Object [])null);
				return Integer.valueOf(o.toString());
			} 
			catch (Exception e) {
				return NOT_SUPPORTED_INT;
			}
		}

		@Override
		protected  void setValueToCam(int zoom){
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
								"setZoom", new Class[] {int.class});
				zoom = correctValue(zoom);
				if(MyDebug.DEBUG)Log.i(LOG_TAG, "zoom setValue = " + zoom);
				method.invoke(param, zoom);
				mCamera.setParameters(param);
			} 
			catch (Exception e) {
				if(MyDebug.DEBUG)e.printStackTrace();
			}
		}
		
		public List<Integer>getZoomRatios(){
			if(false == isSupported()){
				return null;
			}
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"getZoomRatios", new Class[] {});
				return (List<Integer>) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return null;
			}
			
		}
		@Override
		public int getMinValue() {
			// TODO Auto-generated method stub
			return MIN_VAL;
		}
		@Override
		public int getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return -1;
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"getZoom", new Class[] {});
				return (Integer) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return -1;
			}
		}
	}

	public class CameraSettingExposurecompensation extends CameraSettingIntInRange
													implements SetPreference{
		private static final String LOG_TAG = "CameraSettingExposurecompensation";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 8;
		}
		public CameraSettingExposurecompensation() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mExposurecompensation));
			mRangeMin = getMinValue();
			mRangeMax = getMaxValue();
			mDialogTitle = mContext.getString(R.string.exposure_compensation_dialog_title);
			mTitle = mContext.getString(R.string.exposure_compensation_title);
			mSummary = mContext.getString(R.string.exposure_compensation_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			if(!isSupported())return false;
			if(MyDebug.DEBUG) Log.i(LOG_TAG, mKey + " "+
					mRangeMin + " "+
					mRangeMax + " " +
					getValueFromCam());
	        SeekBarPreference seekBarPref = new SeekBarPreference(context);
	        seekBarPref.setDialogTitle(mDialogTitle);
	        seekBarPref.setKey(mKey);
	        seekBarPref.setTitle(mTitle);
	        seekBarPref.setSummary(mSummary);
	        seekBarPref.setRange(mRangeMin, mRangeMax, getValueFromCam());
	        pref.addPreference(seekBarPref);
	        return true;
		}
		
		@Override
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			return (getMinValue() != 0 && getMaxValue() != 0);
		}
		@Override
		public int getMinValue(){
			try{
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
											"getMinExposureCompensation", new Class[] {});
				return (Integer)method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return 0;
			}
		}
		@Override
		public int getMaxValue(){
			try{
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
					"getMaxExposureCompensation", new Class[] {});
				return (Integer)method.invoke(param, (Object[])null);
			} 
			catch (Exception e) {
				return 0;
			}
		}	
		public float getStep(){
			if(false ==  isSupported()){
				return -1;
			}
			try{
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
											"getExposureCompensationStep", new Class[] {});
				return (Float)method.invoke(param, (Object[])null);
			} 
			catch (Exception e) {
				return -1;
			}
		}
		
		@Override
		protected void setValueToCam(int value){
			try{
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"setExposureCompensation", new Class[]{int.class});
				value = correctValue(value);
				method.invoke(param, value);
				mCamera.setParameters(param);
			} 
			catch(Exception e){
				if(MyDebug.DEBUG)e.printStackTrace();
			}
		}
		@Override
		public int getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return -1;
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"getExposureCompensation", new Class[] {});
				return (Integer) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return -1;
			}
		}
	}	

	
	private abstract class CameraSettingBoolean extends CameraSettingBase{
		protected boolean setPreferenceMain(PreferenceCategory pref, Context context){
			if(!isSupported())return false;
	        CheckBoxPreference checkBoxPref = new CheckBoxPreference(context);
	        checkBoxPref.setKey(mKey);
	        checkBoxPref.setTitle(mTitle);
	        checkBoxPref.setSummary(mSummary);
	        checkBoxPref.setDefaultValue(getValueFromCam());
	        pref.addPreference(checkBoxPref);
	        return true;
		}
		/**
		 * サポートされているかどうか
		 */
		public abstract boolean isSupported();
		
		/**
		 * カメラに設定されている値を取得する
		 * @return
		 */
		public abstract boolean getValueFromCam();
		/**
		 * 指定された値を端末のカメラに設定する
		 * @param value
		 */
		protected abstract void setValueToCam(boolean value);
		/**
		 * Preferenceに登録されている値を端末のカメラに設定する
		 */
		@SuppressWarnings("unused")
		public void setValueToCam(){
			if(!isSupported()) return;
			setValueToCam(getValueFromPref());
		}

		/**
		 * Preferenceに登録されている値を取得する
		 * @return
		 */	
		@SuppressWarnings("unused")
		public boolean getValueFromPref(){
			if(!isSupported()) return false;
			return mSharedPref.getBoolean(mKey, getValueFromCam());
		}
		/**
		 * @param value 
		 * @param setNow　
		 * @return true:書き込めた　false:書き込めなかった(対応していないなど）
		 */
		/**
		 * 指定された値をPreferenceに書き込む。また指定がある場合はカメラに反映させる
		 * @param value 設定する値
		 * @param setToCamNow trueの場合はカメラに反映させる
		 * @return
		 */
		@SuppressWarnings("unused")
		public boolean setValueToPref(boolean value, boolean setToCamNow){
		    if(!isSupported()) return false;
		    Editor ed = mSharedPref.edit();
	    	ed.putBoolean(mKey, value);
			ed.commit();
			if(true == setToCamNow){
				setValueToCam(value);
			}
			return true;
		}
		@Override
		public String printSettingCamValue() {
			// TODO Auto-generated method stub
			String msg = isSupported()? String.valueOf(getValueFromCam()):mContext.getString(R.string.not_supported);
			return mKey + "(Cam)=" + msg;
		}
		@Override
		public String printSettingPrefValue() {
			// TODO Auto-generated method stub
			String msg = isSupported()? String.valueOf(getValueFromPref()):mContext.getString(R.string.not_supported);
			return mKey + "(Pref)=" + msg;
		}
	}
	public class CameraSettingAutoExposureLock extends CameraSettingBoolean
											implements SetPreference{

		CameraSettingAutoExposureLock(){
			mKey = addCameraID(mContext.getString(R.string.mExposureLock));
			mTitle = mContext.getString(R.string.exposure_lock_title);
			mSummary = mContext.getString(R.string.exposure_lock_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}

		@Override
		public boolean isSupported() {
			// TODO Auto-generated method stub
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			try {
				Method method = Camera.Parameters.class.getMethod("isAutoExposureLockSupported", new Class[]{});
				Object o = method.invoke(mCamera.getParameters(), (Object[])null);
				return (boolean)(Boolean)o;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			}
			return false;
		}

		@Override
		public boolean getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return false;
			try {
				Method method = Camera.Parameters.class.getMethod("getAutoExposureLock", new Class[]{});
				Object o = method.invoke(mCamera.getParameters(), (Object [])null);
				return (boolean)(Boolean)o;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void setValueToCam(boolean value) {
			// TODO Auto-generated method stub
			if(!isSupported()) return;
			try {
				Method method = Camera.Parameters.class.getMethod("setAutoExposureLock", new Class[]{boolean.class});
				Camera.Parameters param = mCamera.getParameters();
				method.invoke(param, value);
				mCamera.setParameters(param);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			}
		}

		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 14;
		}
	}

	public class CameraSettingVideoStabilization extends CameraSettingBoolean
												implements SetPreference{
		CameraSettingVideoStabilization(){
			mKey = addCameraID(mContext.getString(R.string.mVideoStabilization));
			mTitle = mContext.getString(R.string.video_stabilization_title);
			mSummary = mContext.getString(R.string.video_stabilization_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}

		@Override
		public boolean isSupported() {
			// TODO Auto-generated method stub
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			try {
				Method method = Camera.Parameters.class.getMethod("isVideoStabilizationSupported", new Class[]{});
				Object o = method.invoke(mCamera.getParameters(), (Object[])null);
				return (boolean)(Boolean)o;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			}
			return false;
		}

		@Override
		public boolean getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return false;
			try {
				Method method = Camera.Parameters.class.getMethod("getVideoStabilization", new Class[]{});
				Object o = method.invoke(mCamera.getParameters(), (Object [])null);
				return (boolean)(Boolean)o;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void setValueToCam(boolean value) {
		// TODO Auto-generated method stub
		if(!isSupported()) return;
			try {
				Method method = Camera.Parameters.class.getMethod("setVideoStabilization", new Class[]{boolean.class});
				Camera.Parameters param = mCamera.getParameters();
				method.invoke(param, value);
				mCamera.setParameters(param);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			}
		}

		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 15;
		}
	}
	
	public class CameraSettingAutoWhiteBalanceLock extends CameraSettingBoolean
													implements SetPreference{
		CameraSettingAutoWhiteBalanceLock(){
			mKey = addCameraID(mContext.getString(R.string.mAutoWhiteBalanceLock));
			mTitle = mContext.getString(R.string.auto_white_balance_lock_title);
			mSummary = mContext.getString(R.string.auto_white_balance_lock_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}

		@Override
		public boolean isSupported() {
			// TODO Auto-generated method stub
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			try {
				Method method = Camera.Parameters.class.getMethod("isAutoWhiteBalanceLockSupported", new Class[]{});
				Object o = method.invoke(mCamera.getParameters(), (Object[])null);
				return (boolean)(Boolean)o;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG)e.printStackTrace();
			}
			return false;
		}

		@Override
		public boolean getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return false;
			try {
				Method method = Camera.Parameters.class.getMethod("getAutoWhiteBalanceLock", new Class[]{});
				Object o = method.invoke(mCamera.getParameters(), (Object [])null);
				return (boolean)(Boolean)o;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void setValueToCam(boolean value) {
			// TODO Auto-generated method stub
			if(!isSupported()) return;
			try {
				Method method = Camera.Parameters.class.getMethod("setAutoWhiteBalanceLock", new Class[]{boolean.class});
				Camera.Parameters param = mCamera.getParameters();
				method.invoke(param, value);
				mCamera.setParameters(param);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
			}
		}

		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 14;
		}
	}

	private abstract class CameraSettingIntList extends CameraSettingBase{	
		protected int correctValue(int i){
			i = Math.max(i, 0);
			i = Math.min(i, getSupportedList().size()-1);
			return i;
		}
		protected boolean setPreferenceMain(PreferenceCategory pref, Context context){
			if(!isSupported())return false;
	        ListPreference listPref = new ListPreference(context);
	        listPref.setEntries(stringList2Array(getSupportedListName()));
	        listPref.setEntryValues(stringList2Array(getSupportedListString()));
	        listPref.setDialogTitle(mDialogTitle);
	        listPref.setKey(mKey);
	        listPref.setTitle(mTitle);
	        listPref.setSummary(mSummary);
	        listPref.setDefaultValue(getValueNumFromCam());

	        pref.addPreference(listPref);
	        return true;
		}
		/**
		 * サポートされているかどうかを取得する
		 */
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			if(null == getSupportedList()) return false;
			return getSupportedList().size() > 0;
		}
		
		private static final String KEY_HEADER = "X";
		/**
		 * Preference Entry Value用にリストの番号を文字列に変換する。String.valueof()で単に数字を文字にするとうまくいかない
		 * 端末があるっぽいので、頭にXをつけておく
		 * @param number　文字列に変換する数字
		 * @return
		 */
		private String intToStringForPref(int number){
			return KEY_HEADER + number;
		}
		private int stringToIntForPref(String key){
			String s = key.substring(KEY_HEADER.length());
			int ans;
			try{
				ans = Integer.parseInt(s);
			}
			catch(Exception e){
				if(MyDebug.DEBUG)e.printStackTrace();
				ans = 0;//Prefに設定されている値が変だったときは0を入れておく
			}
			return ans;
		}
		/**
		 * サポートされている解像度一覧を取得する。特にソートはされていない
		 * @return
		 */
		public abstract List<int[]> getSupportedList();
		/**
		 * サポートされている解像度一覧を文字列で取得する。
		 * @return
		 */
		public List<String> getSupportedListString(){
			ArrayList<String>tmp = new ArrayList<String>();
			for(int i = 0;i < getSupportedList().size();i++){
				tmp.add(intToStringForPref(i));
			}
			return tmp;
		}
		private String listToString(int []iList){
			String str = "[";
			for(int i = 0;i < iList.length;i++){
				str += String.valueOf(iList[i]);
				if(i != iList.length-1){
					str += ", ";
				}
			}
			str += "]";
			return str;
		}

		/**
		 * サポートされている解像度一覧を、設定メニューに表示する文字列として取得する
		 * @return
		 */
		public List<String> getSupportedListName(){
			ArrayList<String>tmp = new ArrayList<String>();
			for(int[] s:getSupportedList()){
				tmp.add(listToString(s));
			}
			return tmp;
		}
		/**
		 * カメラに設定されている値を取得する
		 * @return
		 */
		public abstract int[] getValueFromCam();
		
		/**
		 * カメラに設定されているサイズが getSupportedList()で返されるリストの何番目かを取得する
		 * @return　リスト番号の文字列。(存在しない場合は"-1"となるが、カメラから取得したあたいを使っているので-1になることは無いはず
		 */
		private String getValueNumFromCam(){
			int[] i = getValueFromCam();
			return intToStringForPref(getSupportedListNumber(i));
		}
		/**
		 * ListPreferenceは選択番号を文字列で管理しているため、その文字列を数字に直す
		 * @return
		 */
		private int getPrefValue(){
			String s = mSharedPref.getString(mKey, getValueNumFromCam());
			int ans = stringToIntForPref(s);
			ans = correctValue(ans);
			return ans;
		}
		public String getValueName(){
			return getSupportedListName().get(getPrefValue());
		}
		/**
		 * 現在Preferenceに設定されている解像度を取得する
		 * @return
		 */
		@SuppressWarnings("unused")
		public int[] getValueFromPref(){
			return getSupportedList().get(getPrefValue());
		}
		/**
		 * 現在Preferenceに設定されている解像度を端末のカメラに反映させる
		 */
		@SuppressWarnings("unused")
		public void setValueToCam(){
			int prefVal = getPrefValue();
			if(MyDebug.DEBUG)Log.w(LOG_TAG, "prefVal = " + prefVal);
			setValueToCam(prefVal);
		}
		/**
		 * 指定された順番にあるカメラの解像度を端末のカメラに反映させる
		 * @param pos
		 */
		protected abstract void setValueToCam(int pos);
		/**
		 * 指定された値の一覧をPreferenceに書きこむ
		 * @param iList　値一覧　
		 * @param setToCamNow true:すぐに端末に設定する
		 * @return
		 */
		@SuppressWarnings("unused")
		public boolean setValueToPref(int [] iList, boolean setToCamNow){
			if(false == isSupported()){
				return false;
			}
			int i = getSupportedListNumber(iList);
		    Editor ed = mSharedPref.edit();
		    //ListPreferenceは選択番号を文字列で管理しているため、文字列にする
		    String str = i < 0 ? getValueNumFromCam():String.valueOf(i);
	    	ed.putString(mKey, str);
			ed.commit();
			if(true == setToCamNow && i >= 0){
				setValueToCam(i);
			}
			return i >= 0;
		}
		private int getSupportedListNumber(int [] iList){
			List<int []>cList = getSupportedList();
			for(int i = 0;i < cList.size();i++){
				if(cList.get(i).length != iList.length) continue;
				int j;
				for(j = 0;j < iList.length;j++){
					if(cList.get(i)[j] != iList[j]) break;
				}
				if(j == iList.length) return i;
			}
			return -1;
		}
		@Override
		public String printSettingCamValue() {
			// TODO Auto-generated method stub
			String msg;
			if(isSupported()){
				msg = listToString(getValueFromCam());
			}
			else{
				msg = mContext.getString(R.string.not_supported);
			}
			return mKey + "(Cam)=" + msg;
		}
		@Override
		public String printSettingPrefValue() {
			// TODO Auto-generated method stub
			String msg;
			if(isSupported()){
				msg = listToString(getValueFromPref());
			}
			else{
				msg = mContext.getString(R.string.not_supported);
			}
			return mKey + "(Pref)=" + msg;
		}
	}

	public class CameraSettingPreviewFpsRange extends CameraSettingIntList
											implements SetPreference{
		public int PREVIEW_FPS_MAX_INDEX = -1;
		public int PREVIEW_FPS_MIN_INDEX = -1;

		CameraSettingPreviewFpsRange(){
			try {
	        	Field f;
				f = Camera.Parameters.class.getDeclaredField("PREVIEW_FPS_MAX_INDEX");
				PREVIEW_FPS_MAX_INDEX = (int)(Integer)f.get(null);
				f = Camera.Parameters.class.getDeclaredField("PREVIEW_FPS_MIN_INDEX");
				PREVIEW_FPS_MIN_INDEX = (int)(Integer)f.get(null);
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mKey = addCameraID(mContext.getString(R.string.mPreviewFpsRange));
			mDialogTitle = mContext.getString(R.string.preview_fps_range_dialog_title);
			mTitle = mContext.getString(R.string.preview_fps_range_title);
			mSummary = mContext.getString(R.string.preview_fps_range_summary);

		}
		@Override
		public List<int[]> getSupportedList() {
			// TODO Auto-generated method stub
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return null;
			try {
				Method method = Camera.Parameters.class.getMethod("getSupportedPreviewFpsRange", new Class[]{});
				Object o = method.invoke(mCamera.getParameters(), (Object [])null);
				return (List<int[]>)o;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public int[] getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported()) return null;
			int []fps = new int[2];
			try {
				Method method = Camera.Parameters.class.getMethod("getPreviewFpsRange", new Class[]{int[].class});
				method.invoke(mCamera.getParameters(), fps);
				return fps;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void setValueToCam(int pos) {
			// TODO Auto-generated method stub
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return;
			try {
				Method method = Camera.Parameters.class.getMethod("setPreviewFpsRange", 
																	new Class[]{int.class, 
																				int.class});
				int []camList = getSupportedList().get(pos);
				Camera.Parameters param = mCamera.getParameters();
				method.invoke(param, camList[PREVIEW_FPS_MIN_INDEX], camList[PREVIEW_FPS_MAX_INDEX]);
				mCamera.setParameters(param);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return;
		}

		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 9;
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
	}

	@TargetApi(14)
	private abstract class CameraSettingCameraArea extends CameraSettingBase{	
		/**
		 * サポートされているかどうかを取得する
		 */
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			return (getMaxNumAreas() > 0);
		}
		public abstract int getMaxNumAreas();

		/**
		 * エリアのリストを文字列に変換する。指定されたリストがnullあるいはエリア数が0の場合は"0"を返す
		 * @param areas　エリアのリスト
		 * @return　文字列
		 */
		private String areaToString(List<Camera.Area> areas){
			String ans;
			if(areas == null){
				ans = "0";
			}
			else{
				ans = String.valueOf(areas.size());
				for(Camera.Area area:areas){
					ans += ("/" + area.rect.left + "," + area.rect.top + "," + area.rect.right + "," + area.rect.bottom + "," + area.weight);
				}
			}
			return ans;
		}
		/**
		 * 文字列からエリアのリストをパースする。一つもエリアがパースされなかった場合はnullを返す
		 * @param areaStr　文字列
		 * @return　パースされたエリア
		 */
		private List<Camera.Area> stringToAreas(String areaStr){
			ArrayList<Camera.Area> areas = null;
			String[] strs = areaStr.split("/");
			try{
				int num = Integer.parseInt(strs[0]);
				if(num == 0) return null;
				for(int i = 1;i < strs.length;i++){
					try{
						String areaInfo[] = strs[i].split(",");
						if(areaInfo.length != 5) continue;
						final int left   = Integer.parseInt(areaInfo[0]);
						final int top    = Integer.parseInt(areaInfo[1]);
						final int right  = Integer.parseInt(areaInfo[2]);
						final int bottom = Integer.parseInt(areaInfo[3]);
						final int weight = Integer.parseInt(areaInfo[4]);
						Camera.Area area = new Camera.Area(new Rect(left, top, right, bottom), weight);
						if(areas == null) areas = new ArrayList<Camera.Area>();
						areas.add(area);
					}
					catch(Exception e){
						if(MyDebug.DEBUG)e.printStackTrace();
						continue;
					}
				}
			}
			catch(Exception e){
				if(MyDebug.DEBUG)e.printStackTrace();
			}
			
			return areas;
		}
		/**
		 * カメラに設定されている値を取得する。設定されている値がない場合はnullを返す
		 * @return
		 */
		public abstract List<Camera.Area> getValueFromCam();
		/**
		 * プレファレンスに設定されているエリアのリストを取得する。
		 * @return
		 */
		public List<Camera.Area> getValueFromPref(){
			String s = mSharedPref.getString(mKey, areaToString(null));
			return stringToAreas(s);
		}
		/**
		 * 現在Preferenceに設定されている解像度を端末のカメラに反映させる
		 */
		@SuppressWarnings("unused")
		public void setValueToCam(){
			setValueToCam(getValueFromPref());
		}
		/**
		 * 指定されたエリアをカメラに反映させる
		 * @param pos
		 */
		protected abstract void setValueToCam(List<Camera.Area> areas);
		/**
		 * 指定された解像度をPreferenceに書きこむ
		 * @param size
		 * 書きこむ解像度。指定された解像度をカメラがサポートしていない場合はデフォルトを書きこむ
		 * @param setNow
		 * true:すぐに端末のカメラに反映させる　false：させない
		 * @return
		 */
		/**
		 * 指定されたエリア一覧をpreferenceに書き込む
		 * @param areas　書き込むエリア一覧
		 * @param setToCamNow　true:すｇ
		 * @return
		 */
		@SuppressWarnings("unused")
		public boolean setValueToPref(List<Camera.Area> areas, boolean setToCamNow){
			if(false == isSupported()){
				return false;
			}
		    Editor ed = mSharedPref.edit();
		    //ListPreferenceは選択番号を文字列で管理しているため、文字列にする
	    	ed.putString(mKey, areaToString(areas));
			ed.commit();
			if(true == setToCamNow){
				setValueToCam(areas);
			}
			return true;
		}

		@Override
		public String printSettingCamValue() {
			// TODO Auto-generated method stub
			String msg;
			if(isSupported()){
				msg = areaToString(getValueFromCam());
			}
			else{
				msg = mContext.getString(R.string.not_supported);
			}
			return mKey + "(Cam)=" + msg;
		}
		@Override
		public String printSettingPrefValue() {
			// TODO Auto-generated method stub
			String msg;
			if(isSupported()){
				msg = areaToString(getValueFromPref());
			}
			else{
				msg = mContext.getString(R.string.not_supported);
			}
			return mKey + "(Pref)=" + msg;
		}
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 14;
		}
	}
	
	@TargetApi(14)
	public class CameraSettingFocusAreas extends CameraSettingCameraArea
										implements SetPreference{

		CameraSettingFocusAreas(){
			mKey = addCameraID(mContext.getString(R.string.mFocusAreas));
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return false;
		}
		@Override
		public int getMaxNumAreas() {
			// TODO Auto-generated method stub
			return mCamera.getParameters().getMaxNumFocusAreas();
		}
		@Override
		public List<Area> getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported())return null;
			return mCamera.getParameters().getFocusAreas();
		}
		@Override
		protected void setValueToCam(List<Area> areas) {
			// TODO Auto-generated method stub
			if(!isSupported()) return;
			Camera.Parameters param = mCamera.getParameters();
			param.setFocusAreas(areas);
			mCamera.setParameters(param);
		}
	}

	@TargetApi(14)
	public class CameraSettingMeteringAreas extends CameraSettingCameraArea
										implements SetPreference{

		CameraSettingMeteringAreas(){
			mKey = addCameraID(mContext.getString(R.string.mMeteringAreas));
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return false;
		}
		@Override
		public int getMaxNumAreas() {
			// TODO Auto-generated method stub
			return mCamera.getParameters().getMaxNumMeteringAreas();
		}
		@Override
		public List<Area> getValueFromCam() {
			// TODO Auto-generated method stub
			if(!isSupported())return null;
			return mCamera.getParameters().getMeteringAreas();
		}
		@Override
		protected void setValueToCam(List<Area> areas) {
			// TODO Auto-generated method stub
			if(!isSupported()) return;
			Camera.Parameters param = mCamera.getParameters();
			param.setMeteringAreas(areas);
			mCamera.setParameters(param);
		}
	}
	
	

	////////////////////////////////////////////////////////////////////////////
	//垂直方向の画角  public float getHorizontalViewAngle () API8
	public float  getHorizontalViewAngle(){
		if(android.os.Build.VERSION.SDK_INT < 8) return -1;
		try{
			Camera.Parameters param;
			param = mCamera.getParameters();
			Method method = Camera.Parameters.class.getMethod("getHorizontalViewAngle", new Class[]{});
			return (Float)method.invoke(param, (Object [])null);
		}
		catch(Exception e){
			if(MyDebug.DEBUG)e.printStackTrace();
			return -1;
		}
	}
	public float getVerticalViewAngle(){
		if(android.os.Build.VERSION.SDK_INT < 8) return -1;
		try{
			Camera.Parameters param;
			param = mCamera.getParameters();
			Method method = Camera.Parameters.class.getMethod("getVerticalViewAngle", new Class[]{});
			return (Float)method.invoke(param, (Object [])null);
		}
		catch(Exception e){
			if(MyDebug.DEBUG)e.printStackTrace();
			return -1;
		}
	}
	public float getFocalLength(){
		if(android.os.Build.VERSION.SDK_INT < 8) return -1;
		try{
			Camera.Parameters param;
			param = mCamera.getParameters();
			Method method = Camera.Parameters.class.getMethod("getFocalLength", new Class[]{});
			return (Float)method.invoke(param, (Object [])null);
		}
		catch(Exception e){
			if(MyDebug.DEBUG)e.printStackTrace();
			return -1;
		}		
	}
    public boolean  isVideoSnapshotSupported(){
    	if(mCamera == null) return false;
		try {

			Method method = Camera.Parameters.class.getMethod("isVideoSnapshotSupported", new Class[]{});			
	    	Boolean b = (Boolean)method.invoke(mCamera.getParameters());
			return (boolean)b;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		}
    	return false;
    }

	@TargetApi(9)
	public boolean getFocusDistances(float[] output){
		if(android.os.Build.VERSION.SDK_INT < 9) return false;
		Camera.Parameters param;
		param = mCamera.getParameters();
		param.getFocusDistances(output);
		return true;
	}
	public void removeGpsData (){
		Camera.Parameters param = mCamera.getParameters();
		param.removeGpsData();
		mCamera.setParameters(param);
	}
	public void setGpsAltitude (double altitude){
		Camera.Parameters param = mCamera.getParameters();
		param.setGpsAltitude(altitude);
		mCamera.setParameters(param);
	}
	public void setGpsLatitude (double latitude){
		Camera.Parameters param = mCamera.getParameters();
		param.setGpsLatitude(latitude);
		mCamera.setParameters(param);
	}
	public void setGpsLongitude (double longitude){
		Camera.Parameters param = mCamera.getParameters();
		param.setGpsLongitude(longitude);
		mCamera.setParameters(param);
	}
	public boolean setGpsProcessingMethod (String processing_method){
		if(android.os.Build.VERSION.SDK_INT < 8) return false;
		try {
			Camera.Parameters param = mCamera.getParameters();
			Method method = Camera.Parameters.class.getMethod("setGpsProcessingMethod", new Class[]{String.class});			
	    	method.invoke(param, processing_method);
			mCamera.setParameters(param);
			return true;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		}
    	return false;
	}
	public void setGpsTimestamp (long timestamp){
		Camera.Parameters param = mCamera.getParameters();
		param.setGpsTimestamp(timestamp);
		mCamera.setParameters(param);
	}
	public void setRotation (int rotation){
		Camera.Parameters param = mCamera.getParameters();
		param.setRotation(rotation);
		mCamera.setParameters(param);
	}
	/**
	 * 端末の向きに合わせてJpegが正しい向きで保存されるように設定する。端末のAPIlevelが8以下の場合は
	 * Camera.Parameters.setRotation()と同じ動作。
	 * @param orientation　端末の向き 0~359。onOrientationChanged()で渡されるorientationの値を入れると正しく動作する。
	 * @return false:入力されたorientationが異常　true:成功
	 */
	public boolean setOrientation(int orientation){
	    if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return false;
	    Object cameraInfo;
		Field f;
		int camOri;
		int facing;
		if(android.os.Build.VERSION.SDK_INT < 9){
			facing = INVALID_CAMERA_ID;
			camOri = 0;
		}
		else{
			try {
				cameraInfo = clazzCameraInfo.newInstance();
				mGetCameraInfo.invoke(null, mCameraID, cameraInfo);
	        	f = clazzCameraInfo.getDeclaredField("facing");
	    		facing = (Integer)f.get(cameraInfo);
	    		f = clazzCameraInfo.getDeclaredField("orientation");
	    		camOri = (Integer)f.get(cameraInfo);
			} 
			catch (Exception e) {
				// TODO Auto-generated catch block
				if(MyDebug.DEBUG) e.printStackTrace();
				facing = INVALID_CAMERA_ID;
				camOri = 0;
			}
		}
		if(MyDebug.DEBUG) Log.d(LOG_TAG, "facing = " + facing + " Camea Orientaion = " + camOri + " orientation = " + orientation);
		
		while(orientation < 0) orientation += 360;
		orientation = (orientation + 45) / 90 * 90;
	    int rotation;
	    if(facing == INVALID_CAMERA_ID || facing == CAMERA_FACING_BACK){
	    	rotation = (camOri + orientation) % 360;
	    }
	    else {
	    	rotation = (camOri - orientation + 360) % 360;
	    }
	    setRotation(rotation);
	    return true;
	}
	
	
	public boolean setRecordingHint (boolean hint){
		if(android.os.Build.VERSION.SDK_INT < 14) return false;
		try {
			Camera.Parameters param = mCamera.getParameters();
			Method method = Camera.Parameters.class.getMethod("setRecordingHint", new Class[]{boolean.class});			
	    	method.invoke(param, hint);
			mCamera.setParameters(param);
			return true;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		}
    	return false;
	}

	public int getMaxNumDetectedFaces(){
		if(android.os.Build.VERSION.SDK_INT < 14) return 0;
		try {
			Camera.Parameters param = mCamera.getParameters();
			Method method = Camera.Parameters.class.getMethod("getMaxNumDetectedFaces", new Class[]{});			
			Object o = method.invoke(param, (Object[])null);
			return (int)(Integer)o;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG) e.printStackTrace();
		}catch (Exception e){
			if(MyDebug.DEBUG) e.printStackTrace();
		}
    	return 0;
	}

	////////////////////////////////////////////////////////////////////////////

	private static final String LOG_TAG = "CameraSetting";
	private static SharedPreferences mSharedPref = null;
	private Context mContext;
	private Camera mCamera = null;
	private int mCameraID;
	
	public static final String NOT_SUPPORTED_STRING = "";
	public static final int NOT_SUPPORTED_INT = Integer.MIN_VALUE;

	public CameraSettingAntibanding mAntibanding = null;
	public CameraSettingFlashMode mFlashMode = null;
	public CameraSettingColorEffect mColorEffect = null;
	public CameraSettingFocusMode mFocusMode = null;
	public CameraSettingJpegThumbnailSize mJpegThumbnailSize = null;
	public CameraSettingPictureFormat mPictureFormat = null;
	public CameraSettingPreviewFormat mPreviewFormat = null;
	
	public CameraSettingPictureSize mPictureSize = null;
	public CameraSettingPreviewSize mPreviewSize = null;

	public CameraSettingZoom mZoom = null;	
	public CameraSettingSceneMode mSceneMode = null;
	public CameraSettingVideoSize mVideoSize = null;	
	public CameraSettingWhiteBalance mWhiteBalance = null;
	public CameraSettingAutoExposureLock mAutoExposureLock = null;
	public CameraSettingVideoStabilization mVideoStabilization = null;
	public CameraSettingAutoWhiteBalanceLock mAutoWhiteBalanceLock = null;
	public CameraSettingPreviewFrameRate mPreviewFrameRate = null;
	public CameraSettingPreviewFpsRange mPreviewFpsRange = null;
	public CameraSettingFocusAreas mFocusAreas = null;
	public CameraSettingMeteringAreas mMeteringAreas = null;
	
	
	
	
	
	
	
	
	public CameraSettingJpegThumbnailQuality mJpegThumbnailQuality = null;
	public CameraSettingJpegQuality mJpegQuality = null;
	

	
	
	public CameraSettingExposurecompensation mExposurecompensation = null;//public float getExposureCompensationStep () API8

	public ArrayList <SetPreference> mSettingPreferenceList = new ArrayList<SetPreference>();
	public ArrayList<CameraSettingBase> mCameraSettingList = new ArrayList<CameraSettingBase>();

	private static CameraAndParameters mCameraSetting = null;;
	private CameraAndParameters(Context context){
		mContext = context;
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	private void setCameraPos(int camPos){
		if(false == hasMultipleCamera()) camPos = CAMERA_FACING_UNKNOWN;
		
	    Editor ed = mSharedPref.edit();
	    //ListPreferenceは選択番号を文字列で管理しているため、文字列にする
    	ed.putInt(mContext.getString(R.string.mCameraPos), camPos);
		ed.commit();
	}
	
	private int getCameraPos(){
		if(false == hasMultipleCamera())return CAMERA_FACING_UNKNOWN;
		return mSharedPref.getInt(mContext.getString(R.string.mCameraPos), CAMERA_FACING_BACK);
	}

	public SharedPreferences getPreference(){
		return mSharedPref;
	}

	/**
	 * キーにカメラIDを付加する
	 * @param key
	 * @return
	 */
	public String addCameraID(String key){
		return key + "_" + String.valueOf(getCameraPos());
	}
	
	
    public static final int INVALID_CAMERA_ID = -1;
    private static Class<?> clazzCameraInfo = null;
    private static Method mGetNumberOfCameras = null;
    private static Method mGetCameraInfo = null;

    private static Method mOpen = null;

    private static int IMG_FORMAT_JPEG;
    private static int IMG_FORMAT_NV16;
    private static int IMG_FORMAT_NV21;
    private static int IMG_FORMAT_RGB_565;
    private static int IMG_FORMAT_UNKNOWN;
    private static int IMG_FORMAT_YUY2;
    private static int IMG_FORMAT_YV12;
    
    public static int FOCUS_DISTANCE_NEAR_INDEX;
    public static int FOCUS_DISTANCE_OPTIMAL_INDEX;
    public static int FOCUS_DISTANCE_FAR_INDEX;
    
    public static final int CAMERA_FACING_UNKNOWN = -1;
    public static int CAMERA_FACING_BACK = 0;
    public static int CAMERA_FACING_FRONT = 1;
    
	//ビデオスナップショット     isVideoSnapshotSupported () //API14

    static {  
        initCompatibility();  
    };  
  
    private static void initCompatibility() {
    	//ImageFormat
    	try{
    		IMG_FORMAT_JPEG = -1;
    	    IMG_FORMAT_NV16 = -1;
    	    IMG_FORMAT_NV21 = -1;
    	    IMG_FORMAT_RGB_565 = -1;
    	    IMG_FORMAT_UNKNOWN = -1;
    	    IMG_FORMAT_YUY2 = -1;
    	    IMG_FORMAT_YV12 = -1;

    		Class<?> imgFormatClazz = Class.forName("android.graphics.ImageFormat");
    		try{
    			Field f = imgFormatClazz.getDeclaredField("JPEG");
    			IMG_FORMAT_JPEG = (Integer)f.get(null);
    		}
    		catch(Exception e){
    			if(MyDebug.DEBUG)e.printStackTrace();
    		}
    		try{
    			Field f = imgFormatClazz.getDeclaredField("NV16");
    			IMG_FORMAT_NV16 = (Integer)f.get(null);
    		}
    		catch(Exception e){
    			if(MyDebug.DEBUG)e.printStackTrace();
    		}
    		try{
    			Field f = imgFormatClazz.getDeclaredField("NV21");
    			IMG_FORMAT_NV21 = (Integer)f.get(null);
    		}
    		catch(Exception e){
    			if(MyDebug.DEBUG)e.printStackTrace();
    		}
    		try{
    			Field f = imgFormatClazz.getDeclaredField("RGB_565");
    			IMG_FORMAT_RGB_565 = (Integer)f.get(null);
    		}
    		catch(Exception e){
    			if(MyDebug.DEBUG)e.printStackTrace();
    		}
    		try{
    			Field f = imgFormatClazz.getDeclaredField("UNKNOWN");
    			IMG_FORMAT_UNKNOWN = (Integer)f.get(null);
    		}
    		catch(Exception e){
    			if(MyDebug.DEBUG)e.printStackTrace();
    		}
    		try{
    			Field f = imgFormatClazz.getDeclaredField("YUY2");
    			IMG_FORMAT_YUY2 = (Integer)f.get(null);
    		}
    		catch(Exception e){
    			if(MyDebug.DEBUG)e.printStackTrace();
    		}
    		
    		try{
    			Field f = imgFormatClazz.getDeclaredField("YV12");
    			IMG_FORMAT_YV12 = (Integer)f.get(null);
    		}
    		catch(Exception e){
    			if(MyDebug.DEBUG)e.printStackTrace();
    		}
    	}
		catch(Exception e){
			if(MyDebug.DEBUG)e.printStackTrace();
		}
    	
        try{
        	mGetNumberOfCameras = Camera.class
        			.getMethod("getNumberOfCameras", new Class[] {});
        }
        catch (NoSuchMethodException nsme) {
        	mGetNumberOfCameras = null;
        }

        try{
        	clazzCameraInfo = Class.forName("android.hardware.Camera$CameraInfo");
        	Field f = clazzCameraInfo.getDeclaredField("CAMERA_FACING_BACK");
        	CAMERA_FACING_BACK = (Integer)f.get(null);
        	f = clazzCameraInfo.getDeclaredField("CAMERA_FACING_FRONT");
        	CAMERA_FACING_FRONT = (Integer)f.get(null);
        	if(MyDebug.DEBUG){
        		Log.d(LOG_TAG, "FrontID = " + String.valueOf(CAMERA_FACING_FRONT) + " BackID = " + String.valueOf(CAMERA_FACING_BACK));
        	}
        }
        catch (Exception e) {
        	if(MyDebug.DEBUG)e.printStackTrace();
        }
        
        try{
        	mOpen = Camera.class.getMethod("open", new Class[]{int.class});
        }
        catch(NoSuchMethodException nsme) {
        	mOpen = null;
        	if(MyDebug.DEBUG) nsme.printStackTrace();
        }
        try{
        	mGetCameraInfo = Camera.class
        			.getMethod("getCameraInfo", new Class[]{
        											int.class,
        											clazzCameraInfo});
        }
        catch(NoSuchMethodException e){
        	mGetCameraInfo = null;
        	if(MyDebug.DEBUG) e.printStackTrace();
        }
        try{
        	Field f = Camera.Parameters.class.getDeclaredField("FOCUS_DISTANCE_NEAR_INDEX");
        	FOCUS_DISTANCE_NEAR_INDEX = (Integer)f.get(null);
        	f = Camera.Parameters.class.getDeclaredField("FOCUS_DISTANCE_OPTIMAL_INDEX");
        	FOCUS_DISTANCE_OPTIMAL_INDEX = (Integer)f.get(null);
        	f = Camera.Parameters.class.getDeclaredField("FOCUS_DISTANCE_FAR_INDEX");
        	FOCUS_DISTANCE_FAR_INDEX = (Integer)f.get(null);
        }
        catch(Exception e){
        	if(MyDebug.DEBUG)e.printStackTrace();
        	FOCUS_DISTANCE_NEAR_INDEX = -1;
        	FOCUS_DISTANCE_OPTIMAL_INDEX = -1;
        	FOCUS_DISTANCE_FAR_INDEX = -1;
        }
    }
    
    /**
     * 指定されたカメラポジションのカメラIDを取得する
     * @param camPos　// CAMERA_FACING_BACK or CAMERA_FACING_FRONT
     * @return　カメラID。指定されたポジションのカメラがない場合はINALID_CAMERA_IDを返す
     */
    public static int cameraPos2ID(int camPos){
    	if(CAMERA_FACING_UNKNOWN == camPos){
			return INVALID_CAMERA_ID;
    	}

    	Object cameraInfo;
		try {
			cameraInfo = clazzCameraInfo.newInstance();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			if(MyDebug.DEBUG)e.printStackTrace();
			return INVALID_CAMERA_ID;
		}
    	for(int i = 0;i < getNumberOfCameras();i++){
    		Field f;
    		try {
				mGetCameraInfo.invoke(null, i, cameraInfo);
	        	f = clazzCameraInfo.getDeclaredField("facing");
    		} 
    		catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return INVALID_CAMERA_ID;
			}
        	try{
        		int facing = (Integer)f.get(cameraInfo);
        		if(camPos == facing){
        			return i;
        		}
        	}
    		catch(Exception e){
    			if(MyDebug.DEBUG) e.printStackTrace();
    		}
    	}
    	return INVALID_CAMERA_ID;
    }	

    /**
     * 内蔵カメラの個数を返す
     * @return
     * -1:複数カメラを未サポート(APIレベル　8以下)
     * 0以上：カメラの数
     */
    public static int getNumberOfCameras(){
    	
    	if(null == mGetNumberOfCameras){
    		return -1;
    	}
    	int ans = -1;
		try{
			Object o = mGetNumberOfCameras.invoke(null);
			ans = Integer.valueOf(o.toString());
		}
		catch(Exception e){
			if(MyDebug.DEBUG)e.printStackTrace();
			ans = -1;
		}
		return ans;
	}
    
    /**
     * 複数のカメラを持っているかどうか
     * @return
     * true:持っている　false:持っていない
     */
    public static boolean hasMultipleCamera(){
    	return getNumberOfCameras() >= 2;
    }
    
	public static CameraAndParameters newInstance(Context context) { 
		if(mCameraSetting == null){
			mCameraSetting = new CameraAndParameters(context);  
		}
        return mCameraSetting;  
	}
	public Camera openCamera(){
		return openCamera(CAMERA_FACING_BACK);
	}
    /**
     * カメラを開く
     * @return
     */
    public Camera openCamera(int camFacing){

    	releaseCamera();
    	final int cameraNum = getNumberOfCameras();
    	if(cameraNum == 0) return null;//カメラを持っていない
    	try{
        	if(camFacing == CAMERA_FACING_BACK){
    			mCamera = Camera.open();//AndroidではCamera.open()は必ずBackを開く仕様となっている
    			mCameraID = cameraPos2ID(CAMERA_FACING_BACK);
        	}
        	else if(null == mOpen){
    			mCamera = null;
    		}
        	else{
        		int id = cameraPos2ID(camFacing);
        		mCamera = (Camera)mOpen.invoke(null, id);
       			mCameraID = id;
        	}
    	}
    	catch(Exception e){
    		try{
        		e.getCause().printStackTrace();
    		}
    		catch(Exception e2){
    			e2.printStackTrace();
    		}
    		mCamera = null;
    	}
    	if(mCamera == null){
			mCameraID = INVALID_CAMERA_ID;
    		return null;
    	}
    		
   		//カメラ設定情報を作成
   		mAntibanding = new CameraSettingAntibanding();
   		mColorEffect = new CameraSettingColorEffect();
   		mFlashMode = new CameraSettingFlashMode();
   		mFocusMode = new CameraSettingFocusMode();
   		mSceneMode = new CameraSettingSceneMode();
   		mWhiteBalance = new CameraSettingWhiteBalance();
   		mPictureFormat = new CameraSettingPictureFormat();
   		mPictureSize = new CameraSettingPictureSize();
   		mPreviewFormat = new CameraSettingPreviewFormat();
   		mPreviewSize = new CameraSettingPreviewSize();
   		mJpegThumbnailSize = new CameraSettingJpegThumbnailSize();
   		mJpegQuality = new CameraSettingJpegQuality();
   		mVideoSize = new CameraSettingVideoSize();
   		mZoom = new CameraSettingZoom();
   		mExposurecompensation = new CameraSettingExposurecompensation();
   		mJpegThumbnailQuality = new CameraSettingJpegThumbnailQuality();
   		mAutoExposureLock = new CameraSettingAutoExposureLock();
   		mVideoStabilization = new CameraSettingVideoStabilization();
   		mAutoWhiteBalanceLock = new CameraSettingAutoWhiteBalanceLock();
   		mPreviewFrameRate = new CameraSettingPreviewFrameRate();
   		mPreviewFpsRange = new CameraSettingPreviewFpsRange();
   		mFocusAreas = new CameraSettingFocusAreas();
   		mMeteringAreas = new CameraSettingMeteringAreas();

   		//カメラ設定一欄をListに登録
   		mCameraSettingList.add(mColorEffect);
   		mCameraSettingList.add(mSceneMode);
   		mCameraSettingList.add(mPictureSize);
   		mCameraSettingList.add(mVideoSize);
   		mCameraSettingList.add(mJpegQuality);
   		mCameraSettingList.add(mJpegThumbnailSize);
   		mCameraSettingList.add(mZoom);
   		mCameraSettingList.add(mExposurecompensation);
   		mCameraSettingList.add(mFlashMode);
   		mCameraSettingList.add(mFocusMode);
   		mCameraSettingList.add(mWhiteBalance);
   		mCameraSettingList.add(mAntibanding);
   		mCameraSettingList.add(mPictureFormat);
   		mCameraSettingList.add(mPreviewFormat);
   		mCameraSettingList.add(mPreviewSize);
   		mCameraSettingList.add(mJpegThumbnailQuality);
   		mCameraSettingList.add(mAutoExposureLock);
   		mCameraSettingList.add(mVideoStabilization);
   		mCameraSettingList.add(mAutoWhiteBalanceLock);
   		mCameraSettingList.add(mPreviewFrameRate);
   		mCameraSettingList.add(mPreviewFpsRange);
   		mCameraSettingList.add(mFocusAreas);
   		mCameraSettingList.add(mMeteringAreas);
   		
   		//カメラPreference設定一欄をListに登録
   		mSettingPreferenceList.add(mColorEffect);
   		mSettingPreferenceList.add(mSceneMode);
   		mSettingPreferenceList.add(mPictureSize);
   		mSettingPreferenceList.add(mVideoSize);
   		mSettingPreferenceList.add(mJpegQuality);
   		mSettingPreferenceList.add(mJpegThumbnailSize);
   		mSettingPreferenceList.add(mZoom);
   		mSettingPreferenceList.add(mExposurecompensation);
   		mSettingPreferenceList.add(mFlashMode);
   		mSettingPreferenceList.add(mFocusMode);
   		mSettingPreferenceList.add(mWhiteBalance);
   		mSettingPreferenceList.add(mAntibanding);
   		mSettingPreferenceList.add(mPictureFormat);
   		mSettingPreferenceList.add(mPreviewFormat);
   		mSettingPreferenceList.add(mPreviewSize);
   		mSettingPreferenceList.add(mJpegThumbnailQuality);
   		mSettingPreferenceList.add(mAutoExposureLock);
   		mSettingPreferenceList.add(mVideoStabilization);
   		mSettingPreferenceList.add(mAutoWhiteBalanceLock);
   		mSettingPreferenceList.add(mPreviewFrameRate);
   		mSettingPreferenceList.add(mPreviewFpsRange);
   		mSettingPreferenceList.add(mFocusAreas);
   		mSettingPreferenceList.add(mMeteringAreas);

   		return getCamera();
    }
    /**
     * 開かれているカメラを取得する
     * @return
     */
    public Camera getCamera(){
    	return mCamera;
    }
    /**
     * カメラを閉じる
     */
    public void releaseCamera(){
    	if(null!= mSettingPreferenceList){
    		mSettingPreferenceList.clear();
    	}
    	if(null != mCameraSettingList){
    		mCameraSettingList.clear();
    	}
		mAntibanding = null;
		mColorEffect = null;
		mFlashMode = null;
		mFocusMode = null;
		mSceneMode = null;
		mWhiteBalance = null;
		mPictureSize = null;
		mJpegThumbnailSize = null;
		mJpegQuality = null;
		mVideoSize = null;
		mZoom = null;
		mExposurecompensation = null;
		
    	if(null != mCamera){
    		mCamera.release();
    		mCamera = null;
    	}
    }
    public boolean isCameraOpen(){
    	return null != mCamera;
    }
    
}
