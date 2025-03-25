/********************************************************/
/**/
/********************************************************/

/*
AutoEnhace_.java

概要：timelapse画像において１枚目(t=1)の画像の輝度を元にそれに合わせるように輝度を調整する


更新履歴
2014.8.14 version1.0
20160610 version 2.0
	->ほぼ完全に設計見直し
20160614 version 2.1 
 	->並列化による速度アップ

20230130 案
	RGB画像の場合、HyperStack-Color(Composite)に変換して処理すると自然ではなくなる。(色味が変わる)
	*各RGBがそれぞれで処理される弊害に感じる。
	*HSBに変換して、Brightnessのみ処理してもとに戻す？
20230131
	RGB画像でHSB変換後Brightnessを処理後、RGBに戻す方法で実装。
	また、以前のDuplicateからの変更への対応
	*cropされないので、新しい方のメソッドを使用

20230607
	z軸のみの場合、やt軸においても一番暗いところに合わせるほうが都合の良い場合がある。
	これを解消する機能も追加するべき。

@author		hwada
*/
import hw.autoenhance.CalEnhance;
import ij.*;
import ij.plugin.*;

public class AutoEnhance_ implements PlugIn {
	
	public void run( String arg ) {
		//現在のイメージの取得
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.noImage();
			return;
		}

		int nSlices = imp.getStackSize();
		if (nSlices < 2) {
			IJ.showMessage("A stack image ( more than 2 frame )is required.");
			return;
		}
		
		CalEnhance enhancer =  new CalEnhance(imp);
		ImagePlus new_img = enhancer.autoEnhance();
		String fileName = imp.getTitle();
		new_img.setTitle("AutoEnhance_" + fileName);
		new_img.setCalibration(imp.getCalibration());
		new_img.setFileInfo(imp.getOriginalFileInfo());
		new_img.show();
	}
}