package com.thf.AppSwitcher.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;
import com.thf.AppSwitcher.R;

public class FlashButton extends ImageButton {
	private boolean clickable; // = true;

	public enum FlashEnum {
		OFF, ON, AUTO
	}

	public interface FlashListener {
		void onState(FlashEnum state);

	}

	private FlashEnum mState;
	private FlashListener mFlashListener;

	public FlashButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.clickable = true;
		//Sets initial state
		setState(FlashEnum.OFF);
	}

	@Override
	public boolean performClick() {

		super.performClick();
		if (this.clickable) {
			int next = ((mState.ordinal() + 1) % FlashEnum.values().length);
			setState(FlashEnum.values()[next]);
			performFlashClick();
			return true;
		} else {
			return false;
		}
	}

	private void performFlashClick() {
		if (mFlashListener == null)
			return;
		mFlashListener.onState(mState);
		/*
		switch (mState) {
			case OFF:
			mFlashListener.on();
			break;
			case WHITE:
			mFlashListener.onOn();
			break;
			case YELLOW:
			mFlashListener.onOff();
			break;
			
		
		}
		*/
	}

	private void createDrawableState() {
		switch (mState) {
		case OFF:
			setImageResource(R.drawable.sun_up);
			break;
		case ON:
			setImageResource(R.drawable.moon_up);
			break;
		case AUTO:
			setImageResource(R.drawable.sun_moon_up);
			break;

		}
	}

	public FlashEnum getState() {
		return mState;
	}

	public void setState(FlashEnum state) {
		if (state == null)
			return;
		this.mState = state;
		createDrawableState();

	}

	public FlashListener getFlashListener() {
		return mFlashListener;
	}

	public void setFlashListener(FlashListener flashListener) {
		this.mFlashListener = flashListener;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = getMeasuredWidth();
		setMeasuredDimension(width, width);
	}

	@Override
	public void setEnabled(boolean b) {
		if (b) {
			setImageAlpha(255);
			this.clickable = true;
		} else {
			setImageAlpha(100);
			this.clickable = false;
		}

	}

}