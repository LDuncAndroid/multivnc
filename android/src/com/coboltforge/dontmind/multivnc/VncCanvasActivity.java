/* 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

//
// CanvasView is the Activity for showing VNC Desktop.
//
package com.coboltforge.dontmind.multivnc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

public class VncCanvasActivity extends Activity {


	public class TouchpadInputHandler extends AbstractGestureInputHandler {
		/**
		 * In drag mode (entered with long press) you process mouse events
		 * without sending them through the gesture detector
		 */
		private boolean dragMode;
		float dragX, dragY;
		
		private boolean button2insteadof1 = false;
		
		private long twoFingerFlingStart = -1;
		private VelocityTracker twoFingerFlingVelocityTracker;
		private boolean twoFingerFlingDetected = false;
		private final int TWO_FINGER_FLING_UNITS = 1000;
		private final float TWO_FINGER_FLING_THRESHOLD = 1000;
		
		TouchpadInputHandler() {
			super(VncCanvasActivity.this);
			twoFingerFlingVelocityTracker = VelocityTracker.obtain();
		}
		
		@Override
		protected void finalize() throws Throwable {
			if(twoFingerFlingVelocityTracker != null)
				twoFingerFlingVelocityTracker.recycle();
			
			Log.d(TAG, "TouchpadInputHandler destroyed!");
			super.finalize();
		}

		
		/**
		 * scale down delta when it is small. This will allow finer control
		 * when user is making a small movement on touch screen.
		 * Scale up delta when delta is big. This allows fast mouse movement when
		 * user is flinging.
		 * @param deltaX
		 * @return
		 */
		private float fineCtrlScale(float delta) {
			float sign = (delta>0) ? 1 : -1;
			delta = Math.abs(delta);
			if (delta>=1 && delta <=3) {
				delta = 1;
			}else if (delta <= 10) {
				delta *= 0.34;
			} else if (delta <= 30 ) {
				delta *= delta/30;
			} else if (delta <= 90) {
				delta *=  (delta/30);
			} else {
				delta *= 3.0;
			}
			return sign * delta;
		}
		
		private void twoFingerFlingNotification(String str)
		{
			// bzzt!	
			vncCanvas.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
					HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING|HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);

			// beep!
			vncCanvas.playSoundEffect(SoundEffectConstants.CLICK);
			
			VncCanvasActivity.this.notificationToast.setText(str);
			VncCanvasActivity.this.notificationToast.show();
		}
		
		private void twoFingerFlingAction(Character d)
		{
			switch(d) {
			case '←':
				vncCanvas.sendMetaKey(MetaKeyBean.keyArrowLeft);
				break;
			case '→':
				vncCanvas.sendMetaKey(MetaKeyBean.keyArrowRight);
				break;
			case '↑':
				vncCanvas.sendMetaKey(MetaKeyBean.keyArrowUp);
				break;
			case '↓':
				vncCanvas.sendMetaKey(MetaKeyBean.keyArrowDown);
				break;
			}
		}
		

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
		 */
		@Override
		public void onLongPress(MotionEvent e) {

			if(Utils.DEBUG()) Log.d(TAG, "Input: long press");
			
			showZoomer(true);
			
			// disable if virtual mouse buttons are in use 
			if(mousebuttons.getVisibility()== View.VISIBLE)
				return;
			
			vncCanvas.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
					HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING|HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
			dragMode = true;
			dragX = e.getX();
			dragY = e.getY();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent,
		 *      android.view.MotionEvent, float, float)
		 */
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			
			if (e2.getPointerCount() > 1)
			{
				if(Utils.DEBUG()) Log.d(TAG, "Input: scroll multitouch");
				
				if (inScaling)
					return false;
				
				// pan on 3 fingers and more
				if(e2.getPointerCount() > 2) {
					showZoomer(false);
					return vncCanvas.pan((int) distanceX, (int) distanceY);
				}
			
				/*
				 * here comes the stuff that acts for two fingers
				 */

				// gesture start
				if(twoFingerFlingStart != e1.getEventTime()) // it's a new scroll sequence
				{
					twoFingerFlingStart = e1.getEventTime();
					twoFingerFlingDetected = false;
					twoFingerFlingVelocityTracker.clear();
					if(Utils.DEBUG()) Log.d(TAG, "new twoFingerFling detection started");
				}
				
				// gesture end
				if(twoFingerFlingDetected == false) // not yet detected in this sequence (we only want ONE event per sequence!)
				{
					// update our velocity tracker
					twoFingerFlingVelocityTracker.addMovement(e2);
					twoFingerFlingVelocityTracker.computeCurrentVelocity(TWO_FINGER_FLING_UNITS);
					
					float velocityX = twoFingerFlingVelocityTracker.getXVelocity();
					float velocityY = twoFingerFlingVelocityTracker.getYVelocity();

					// check for left/right flings
					if(Math.abs(velocityX) > TWO_FINGER_FLING_THRESHOLD
							&& Math.abs(velocityX) > Math.abs(2*velocityY)) {
						if(velocityX < 0) {
							if(Utils.DEBUG()) Log.d(TAG, "twoFingerFling LEFT detected");
							twoFingerFlingNotification("←");
							twoFingerFlingAction('←');
						}
						else {
							if(Utils.DEBUG()) Log.d(TAG, "twoFingerFling RIGHT detected");
							twoFingerFlingNotification("→");
							twoFingerFlingAction('→');
						}

						twoFingerFlingDetected = true;
					}

					// check for left/right flings
					if(Math.abs(velocityY) > TWO_FINGER_FLING_THRESHOLD
							&& Math.abs(velocityY) > Math.abs(2*velocityX)) {
						if(velocityY < 0) {
							if(Utils.DEBUG()) Log.d(TAG, "twoFingerFling UP detected");
							twoFingerFlingNotification("↑");
							twoFingerFlingAction('↑');
						}
						else {
							if(Utils.DEBUG()) Log.d(TAG, "twoFingerFling DOWN detected");
							twoFingerFlingNotification("↓");
							twoFingerFlingAction('↓');
						}

						twoFingerFlingDetected = true;
					}
					
				}

				return true;
			}
			else
			{
				// compute the relative movement offset on the remote screen.
				float deltaX = -distanceX *vncCanvas.getScale();
				float deltaY = -distanceY *vncCanvas.getScale();
				deltaX = fineCtrlScale(deltaX);
				deltaY = fineCtrlScale(deltaY);
	
				// compute the absolution new mouse pos on the remote site.
				float newRemoteX = vncCanvas.mouseX + deltaX;
				float newRemoteY = vncCanvas.mouseY + deltaY;
	
				if(Utils.DEBUG()) Log.d(TAG, "Input: scroll single touch from " 
						+ vncCanvas.mouseX + "," + vncCanvas.mouseY
						+ " to " + (int)newRemoteX + "," + (int)newRemoteY);
	
				if (dragMode) {
					if (e2.getAction() == MotionEvent.ACTION_UP)
						dragMode = false;
					dragX = e2.getX();
					dragY = e2.getY();
					e2.setLocation(newRemoteX, newRemoteY);
					return vncCanvas.processPointerEvent(e2, true);
				} else {
						e2.setLocation(newRemoteX, newRemoteY);
						vncCanvas.processPointerEvent(e2, false);
				}
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.coboltforge.dontmind.multivnc.AbstractGestureInputHandler#onTouchEvent(android.view.MotionEvent)
		 */
		@Override
		public boolean onTouchEvent(MotionEvent e) {
			if (dragMode) {
				
				if(Utils.DEBUG()) Log.d(TAG, "Input: touch drag");

				// compute the relative movement offset on the remote screen.
				float deltaX = (e.getX() - dragX) *vncCanvas.getScale();
				float deltaY = (e.getY() - dragY) *vncCanvas.getScale();
				dragX = e.getX();
				dragY = e.getY();
				deltaX = fineCtrlScale(deltaX);
				deltaY = fineCtrlScale(deltaY);

				// compute the absolution new mouse pos on the remote site.
				float newRemoteX = vncCanvas.mouseX + deltaX;
				float newRemoteY = vncCanvas.mouseY + deltaY;


				if (e.getAction() == MotionEvent.ACTION_UP)
				{
					if(Utils.DEBUG()) Log.d(TAG, "Input: touch drag, finger up");
					
					dragMode = false;
					
					button2insteadof1 = false;

					remoteMouseStayPut(e);
					vncCanvas.processPointerEvent(e, false);
					return super.onTouchEvent(e); // important! otherwise the gesture detector gets confused!
				}
				e.setLocation(newRemoteX, newRemoteY);
				
				boolean status = false;
				if(!button2insteadof1) // button 1 down
					status = vncCanvas.processPointerEvent(e, true, false);
				else // button2 down
					status = vncCanvas.processPointerEvent(e, true, true);
				return status;

			} else
			{
				if(Utils.DEBUG())
					Log.d(TAG, "Input: touch normal: x:" + e.getX() + " y:" + e.getY() + " action:" + e.getAction());
							
				return super.onTouchEvent(e);
			}
		}

		/**
		 * Modify the event so that it does not move the mouse on the
		 * remote server.
		 * @param e
		 */
		private void remoteMouseStayPut(MotionEvent e) {
			e.setLocation(vncCanvas.mouseX, vncCanvas.mouseY);
			
		}
		/*
		 * (non-Javadoc)
		 * confirmed single tap: do a left mouse down and up click on remote without moving the mouse.
		 * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapConfirmed(android.view.MotionEvent)
		 */
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			
			// disable if virtual mouse buttons are in use 
			if(mousebuttons.getVisibility()== View.VISIBLE)
				return false;
			
			if(Utils.DEBUG()) Log.d(TAG, "Input: single tap");
			
			boolean multiTouch = e.getPointerCount() > 1;
			remoteMouseStayPut(e);

			vncCanvas.processPointerEvent(e, true, multiTouch||vncCanvas.cameraButtonDown);
			e.setAction(MotionEvent.ACTION_UP);
			return vncCanvas.processPointerEvent(e, false, multiTouch||vncCanvas.cameraButtonDown);
		}

		/*
		 * (non-Javadoc)
		 * double tap: do right mouse down and up click on remote without moving the mouse.
		 * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
		 */
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			
			// disable if virtual mouse buttons are in use 
			if(mousebuttons.getVisibility()== View.VISIBLE)
				return false;
			
			if(Utils.DEBUG()) Log.d(TAG, "Input: double tap");

			button2insteadof1 = true;
			
			remoteMouseStayPut(e);
			vncCanvas.processPointerEvent(e, true, true);
			e.setAction(MotionEvent.ACTION_UP);
			return vncCanvas.processPointerEvent(e, false, true);
		}
		
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
		 */
		@Override
		public boolean onDown(MotionEvent e) {
			panner.stop();
			return true;
		}
	}
	
	private final static String TAG = "VncCanvasActivity";


	VncCanvas vncCanvas;
	VncDatabase database;
	private ConnectionBean connection;

	ZoomControls zoomer;
	Panner panner;
	TouchpadInputHandler touchPad;
	ViewGroup mousebuttons;
	TouchPointView touchpoints;
	Toast notificationToast;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		database = new VncDatabase(this);

		Intent i = getIntent();
		connection = new ConnectionBean();
		Uri data = i.getData();
		if ((data != null) && (data.getScheme().equals("vnc"))) {
			String host = data.getHost();
			// This should not happen according to Uri contract, but bug introduced in Froyo (2.2)
			// has made this parsing of host necessary
			int index = host.indexOf(':');
			int port;
			if (index != -1)
			{
				try
				{
					port = Integer.parseInt(host.substring(index + 1));
				}
				catch (NumberFormatException nfe)
				{
					port = 0;
				}
				host = host.substring(0,index);
			}
			else
			{
				port = data.getPort();
			}
			if (host.equals(VncConstants.CONNECTION))
			{
				if (connection.Gen_read(database.getReadableDatabase(), port))
				{
					MostRecentBean bean = MainMenuActivity.getMostRecent(database.getReadableDatabase());
					if (bean != null)
					{
						bean.setConnectionId(connection.get_Id());
						bean.Gen_update(database.getWritableDatabase());
					}
				}
			}
			else
			{
			    connection.setAddress(host);
			    connection.setNickname(connection.getAddress());
			    connection.setPort(port);
			    List<String> path = data.getPathSegments();
			    if (path.size() >= 1) {
			        connection.setColorModel(path.get(0));
			    }
			    if (path.size() >= 2) {
			        connection.setPassword(path.get(1));
			    }
			    connection.save(database.getWritableDatabase());
			}
		} else {
		
		    Bundle extras = i.getExtras();

		    if (extras != null) {
		  	    connection.Gen_populate((ContentValues) extras
				  	.getParcelable(VncConstants.CONNECTION));
		    }
		    if (connection.getPort() == 0)
			    connection.setPort(5900);

            // Parse a HOST:PORT entry
		    String host = connection.getAddress();
		    if (host.indexOf(':') > -1) {
			    String p = host.substring(host.indexOf(':') + 1);
			    try {
				    connection.setPort(Integer.parseInt(p));
			    } catch (Exception e) {
			    }
			    connection.setAddress(host.substring(0, host.indexOf(':')));
	  	    }
		}
		setContentView(R.layout.canvas);

		vncCanvas = (VncCanvas) findViewById(R.id.vnc_canvas);
		zoomer = (ZoomControls) findViewById(R.id.zoomer);

		vncCanvas.initializeVncCanvas(connection, new Runnable() {
			public void run() {
				setModes();
			}
		});
		zoomer.hide();
		zoomer.setOnZoomInClickListener(new View.OnClickListener() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 */
			@Override
			public void onClick(View v) {
				showZoomer(true);
				vncCanvas.scaling.zoomIn(VncCanvasActivity.this);

			}

		});
		zoomer.setOnZoomOutClickListener(new View.OnClickListener() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 */
			@Override
			public void onClick(View v) {
				showZoomer(true);
				vncCanvas.scaling.zoomOut(VncCanvasActivity.this);

			}

		});
		zoomer.setOnZoomKeyboardClickListener(new View.OnClickListener() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 */
			@Override
			public void onClick(View v) {
              InputMethodManager inputMgr = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
              inputMgr.toggleSoftInput(0, 0);
			}

		});
		panner = new Panner(this, vncCanvas.handler);

		touchPad =  new TouchpadInputHandler();
		
		mousebuttons = (ViewGroup) findViewById(R.id.virtualmousebuttons);
		MouseButtonView mousebutton1 = (MouseButtonView) findViewById(R.id.mousebutton1);
		MouseButtonView mousebutton2 = (MouseButtonView) findViewById(R.id.mousebutton2);
		MouseButtonView mousebutton3 = (MouseButtonView) findViewById(R.id.mousebutton3);
		
		mousebutton1.init(1, vncCanvas);
		mousebutton2.init(2, vncCanvas);
		mousebutton3.init(3, vncCanvas);
		
		touchpoints = (TouchPointView) findViewById(R.id.touchpoints);
		
		// create an empty toast. we do this do be able to cancel
		notificationToast = Toast.makeText(this,  "", Toast.LENGTH_SHORT);
		notificationToast.setGravity(Gravity.TOP, 0, 0);
	}
	
	
	 /* 
     * this is used to load our native libraries. order is important!!!
     */
    static {
    	System.loadLibrary("vncclient");
    	System.loadLibrary("jnivncconn");
    }
	

	/**
	 * Set modes on start to match what is specified in the ConnectionBean;
	 * color mode (already done), scaling
	 */
	void setModes() {
		
		AbstractScaling.getByScaleType(connection.getScaleMode())
				.setScaleTypeForActivity(this);
		
	}

	ConnectionBean getConnection() {
		return connection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		
		
		// Default to meta key dialog
		return new MetaKeyDialog(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (dialog instanceof ConnectionSettable)
			((ConnectionSettable) dialog).setConnection(connection);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation/keyboard change
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onStop() {
		vncCanvas.disableRepaints();
		super.onStop();
	}

	@Override
	protected void onRestart() {
		vncCanvas.enableRepaints();
		super.onRestart();
	}

	/** {@inheritDoc} */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.vnccanvasactivitymenu, menu);
		return true;
	}




	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		vncCanvas.afterMenu = true;
		switch (item.getItemId()) {
		case R.id.itemInfo:
			vncCanvas.showConnectionInfo();
			return true;
		case R.id.itemSpecialKeys:
			showDialog(R.layout.metakey);
			return true;
		case R.id.itemColorMode:
			selectColorModel();
			return true;
		case R.id.itemToggleFramebufferUpdate:
			if(vncCanvas.toggleFramebufferUpdates()) // view enabled
			{
				vncCanvas.setVisibility(View.VISIBLE);
				touchpoints.setVisibility(View.GONE);
			}
			else
			{
				vncCanvas.setVisibility(View.GONE);
				touchpoints.setVisibility(View.VISIBLE);
			}
			return true;	
			
		case R.id.itemToggleMouseButtons:
			if(mousebuttons.getVisibility()== View.VISIBLE)
				mousebuttons.setVisibility(View.GONE);
			else
				mousebuttons.setVisibility(View.VISIBLE);	
			return true;	

		case R.id.itemToggleKeyboard:
			toggleKeyboard();
			return true;
	
		case R.id.itemSendKeyAgain:
			sendSpecialKeyAgain();
			return true;
		case R.id.itemSaveBookmark:
			connection.save(database.getWritableDatabase());
			Toast.makeText(this, getString(R.string.bookmark_saved), Toast.LENGTH_SHORT).show();
			return true;
		case R.id.itemOpenDoc:
			Intent intent = new Intent (this, AboutActivity.class);
			this.startActivity(intent);
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private MetaKeyBean lastSentKey;

	private void sendSpecialKeyAgain() {
		if (lastSentKey == null
				|| lastSentKey.get_Id() != connection.getLastMetaKeyId()) {
			ArrayList<MetaKeyBean> keys = new ArrayList<MetaKeyBean>();
			Cursor c = database.getReadableDatabase().rawQuery(
					MessageFormat.format("SELECT * FROM {0} WHERE {1} = {2}",
							MetaKeyBean.GEN_TABLE_NAME,
							MetaKeyBean.GEN_FIELD__ID, connection
									.getLastMetaKeyId()),
					MetaKeyDialog.EMPTY_ARGS);
			MetaKeyBean.Gen_populateFromCursor(c, keys, MetaKeyBean.NEW);
			c.close();
			if (keys.size() > 0) {
				lastSentKey = keys.get(0);
			} else {
				lastSentKey = new MetaKeyBean();
				lastSentKey.setKeySym(0xFFC6); // set F9 as default
			}
		}
		if (lastSentKey != null)
			vncCanvas.sendMetaKey(lastSentKey);
	}
	
	private void toggleKeyboard() {
		InputMethodManager inputMgr = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMgr.toggleSoftInput(0, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isFinishing()) {
			vncCanvas.closeConnection();
			vncCanvas.onDestroy();
			database.close();
		}
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(touchpoints.getVisibility()== View.VISIBLE)
			touchpoints.handleEvent(event);
		
		return touchPad.onTouchEvent(event);
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent evt) {
		if(Utils.DEBUG()) Log.d(TAG, "Input: key down: " + evt.toString());

		if (keyCode == KeyEvent.KEYCODE_MENU)
			return super.onKeyDown(keyCode, evt);
		
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			
			new AlertDialog.Builder(this)
			.setMessage(getString(R.string.disconnect_question))
			.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					vncCanvas.closeConnection();
					finish();
				}
			}).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Do nothing.
				}
			}).show();
			
			return true;
		}

		// use search key to toggle soft keyboard
		if (keyCode == KeyEvent.KEYCODE_SEARCH)
			toggleKeyboard();

		if (vncCanvas.processLocalKeyEvent(keyCode, evt))
			return true;
		return super.onKeyDown(keyCode, evt);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent evt) {
		if(Utils.DEBUG()) Log.d(TAG, "Input: key up: " + evt.toString());

		if (keyCode == KeyEvent.KEYCODE_MENU)
			return super.onKeyUp(keyCode, evt);

		if (vncCanvas.processLocalKeyEvent(keyCode, evt))
			return true;
		return super.onKeyUp(keyCode, evt);
	}


	private void selectColorModel() {
		// Stop repainting the desktop
		// because the display is composited!
		vncCanvas.disableRepaints();

		final String[] choices = new String[COLORMODEL.values().length];
		int currentSelection = -1;
		for (int i = 0; i < choices.length; i++) {
			COLORMODEL cm = COLORMODEL.values()[i];
			choices[i] = cm.toString();
			if (vncCanvas.isColorModel(cm))
				currentSelection = i;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setSingleChoiceItems(choices, currentSelection, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	dialog.dismiss();
				COLORMODEL cm = COLORMODEL.values()[item];
				vncCanvas.setColorModel(cm);
				connection.setColorModel(cm.nameString());
				Toast.makeText(VncCanvasActivity.this,
						"Updating Color Model to " + cm.toString(),
						Toast.LENGTH_SHORT).show();
		    }
		});
		AlertDialog dialog = builder.create();
		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface arg0) {
				Log.i(TAG, "Color Model Selector dismissed");
				// Restore desktop repaints
				vncCanvas.enableRepaints();
			}
		});
		dialog.show();
	}

	float panTouchX, panTouchY;

	/**
	 * Pan based on touch motions
	 * 
	 * @param event
	 */
	private boolean pan(MotionEvent event) {
		float curX = event.getX();
		float curY = event.getY();
		int dX = (int) (panTouchX - curX);
		int dY = (int) (panTouchY - curY);

		return vncCanvas.pan(dX, dY);
	}

	

	boolean touchPan(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			panTouchX = event.getX();
			panTouchY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			pan(event);
			panTouchX = event.getX();
			panTouchY = event.getY();
			break;
		case MotionEvent.ACTION_UP:
			pan(event);
			break;
		}
		return true;
	}


	long hideZoomAfterMs;
	static final long ZOOM_HIDE_DELAY_MS = 2500;
	HideZoomRunnable hideZoomInstance = new HideZoomRunnable();

	private void showZoomer(boolean force) {
		if (force || zoomer.getVisibility() != View.VISIBLE) {
			zoomer.show();
			hideZoomAfterMs = SystemClock.uptimeMillis() + ZOOM_HIDE_DELAY_MS;
			vncCanvas.handler
					.postAtTime(hideZoomInstance, hideZoomAfterMs + 10);
		}
	}

	private class HideZoomRunnable implements Runnable {
		public void run() {
			if (SystemClock.uptimeMillis() >= hideZoomAfterMs) {
				zoomer.hide();
			}
		}

	}
	
	public void showScaleToast()
	{
		// show scale
		notificationToast.setText(getString(R.string.scale_msg) + " " + (int)(100*vncCanvas.getScale()) + "%");
		notificationToast.show();
	}

	
}
