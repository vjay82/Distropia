package org.distropia.client.gui;

import com.google.gwt.event.shared.HandlerRegistration;
import com.reveregroup.gwt.imagepreloader.Dimensions;
import com.reveregroup.gwt.imagepreloader.FitImageLoadEvent;
import com.reveregroup.gwt.imagepreloader.FitImageLoadHandler;
import com.reveregroup.gwt.imagepreloader.ImageLoadEvent;
import com.reveregroup.gwt.imagepreloader.ImageLoadHandler;
import com.reveregroup.gwt.imagepreloader.ImagePreloader;
import com.smartgwt.client.widgets.Img;

public class MyFitImage extends Img {
	private Integer maxWidth, maxHeight, fixedWidth, fixedHeight;

	private int height = 0;

	private int width = 0;

	private Double aspectRatio;

	private Dimensions dimensions;

	public void resize() {
		if (fixedWidth != null) {
			setWidth(fixedWidth);
			if (fixedHeight != null) {
				setHeight(fixedHeight);
			} else if (aspectRatio != null) {
				setHeight((int) Math.round(fixedWidth * aspectRatio));
			} else {
				setHeight(fixedWidth);
			}
		} else if (fixedHeight != null) {
			setHeight(fixedHeight);
			if (aspectRatio != null) {
				setWidth((int) Math.round(fixedHeight / aspectRatio));
			} else {
				setWidth(fixedHeight);
			}
		} else if (maxWidth != null) {
			if (maxHeight != null) {
				if (aspectRatio != null) {
					double maxAR = ((double) maxHeight) / ((double) maxWidth);
					if (aspectRatio > maxAR) {
						setHeight(maxHeight);
						setWidth((int) Math.round(maxHeight / aspectRatio));
					} else {
						setWidth(maxWidth);
						setHeight((int) Math.round(maxWidth * aspectRatio));
					}
				} else {
					setWidth(maxWidth);
					setHeight(maxHeight);
				}
			} else {
				setWidth(maxWidth);
				if (aspectRatio != null)
					setHeight((int) Math.round(maxWidth * aspectRatio));
				else
					setHeight(maxWidth);
			}
		} else if (maxHeight != null) {
			setHeight(maxHeight);
			if (aspectRatio != null)
				setWidth((int) Math.round(maxHeight / aspectRatio));
			else
				setWidth(maxHeight);
		} else {
			setWidth((Integer) null);
			setHeight((Integer) null);
		}
	}

	public MyFitImage() {
	}

	public MyFitImage(String url) {
		super();
		setSrc(url);
	}

	public MyFitImage(FitImageLoadHandler loadHandler) {
		super();
		addFitImageLoadHandler(loadHandler);
	}

	public MyFitImage(String url, FitImageLoadHandler loadHandler) {
		super();
		addFitImageLoadHandler(loadHandler);
		setSrc(url);
	}

	public MyFitImage(String url, int maxWidth, int maxHeight) {
		super();
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		setSrc(url);
		resize();
	}

	public MyFitImage(String url, int maxWidth, int maxHeight,
			FitImageLoadHandler loadHandler) {
		super();
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		addFitImageLoadHandler(loadHandler);
		setSrc(url);
		resize();
	}

	@Override
	public void setSrc(String url) {
		super.setSrc(url);
		ImagePreloader.load(url, new ImageLoadHandler() {
			public void imageLoaded(ImageLoadEvent event) {
				if (!event.isLoadFailed()) {
					dimensions = event.getDimensions();
					aspectRatio = ((double) dimensions.getHeight())
							/ ((double) dimensions.getWidth());
				}
				resize();
				fireEvent(new FitImageLoadEvent(event.isLoadFailed()));
			}
		});
	}

	public Integer getOriginalWidth() {
		return dimensions == null ? null : dimensions.getWidth();
	}

	public Integer getOriginalHeight() {
		return dimensions == null ? null : dimensions.getHeight();
	}

	/**
	 * <p>
	 * Handle FitImageLoadEvents. These events are fired whenever the image
	 * finishes loading completely or fails to load. The event occurs after the
	 * image has been resized to fit the original image aspect ratio.
	 * 
	 * <p>
	 * NOTE: Add this handler before setting the URL property of the FitImage.
	 * If set after, there is no guarantee that the handler will be fired for
	 * the event.
	 */
	public HandlerRegistration addFitImageLoadHandler(
			FitImageLoadHandler handler) {
		return addHandler(handler, FitImageLoadEvent.getType());
	}

	public Integer getImgMaxWidth() {
		return maxWidth;
	}

	/**
	 * The width of the image will never exceed this number of pixels.
	 */
	public void setImgMaxWidth(Integer maxWidth) {
		this.maxWidth = maxWidth;
		resize();
	}

	public Integer getImgMaxHeight() {
		return maxHeight;
	}

	/**
	 * The height of the image will never exceed this number of pixels.
	 */
	public void setImgMaxHeight(Integer maxHeight) {
		this.maxHeight = maxHeight;
		resize();
	}

	public void setMaxSize(Integer maxWidth, Integer maxHeight) {
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		resize();
	}

	public Integer getFixedWidth() {
		return fixedWidth;
	}

	/**
	 * The exact width (in pixels) for the image. This overrides the max
	 * dimension behavior, but preserves aspect ratio if fixedHeight is not also
	 * specified.
	 */
	public void setFixedWidth(Integer fixedWidth) {
		this.fixedWidth = fixedWidth;
		resize();
	}

	public Integer getFixedHeight() {
		return fixedHeight;
	}

	/**
	 * The exact height (in pixels) for the image. This overrides the max
	 * dimension behavior, but preserves aspect ratio if fixedWidth is not also
	 * specified.
	 */
	public void setFixedHeight(Integer fixedHeight) {
		this.fixedHeight = fixedHeight;
		resize();
	}

	public void setFixedSize(Integer fixedWidth, Integer fixedHeight) {
		this.fixedWidth = fixedWidth;
		this.fixedHeight = fixedHeight;
		resize();
	}

	private void setHeight(Integer px) {
		if (px == null) {
			if (dimensions != null)
				height = dimensions.getHeight();
			else
				height = 1;
		} else
			height = px;
		super.setHeight(height);
	}

	public Integer getHeight() {
		return this.height;
	}

	private void setWidth(Integer px) {
		if (px == null) {
			if (dimensions != null)
				width = dimensions.getWidth();
			else
				width = 1;
		} else
			width = px;
		super.setWidth(width);
	}

	public Integer getWidth() {
		return this.width;
	}
}
