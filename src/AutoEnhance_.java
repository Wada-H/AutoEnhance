/********************************************************/
/**/
/********************************************************/

/*
AutoEnhace_.java

�T�v�Ftimelapse�摜�ɂ����ĂP����(t=1)�̉摜�̋P�x�����ɂ���ɍ��킹��悤�ɋP�x�𒲐�����


�X�V����
2014.8.14 version1.0
20160610 version 2.0
	->�قڊ��S�ɐ݌v������
20160614 version 2.1 
 	->���񉻂ɂ�鑬�x�A�b�v

20230130 ��
	RGB�摜�̏ꍇ�AHyperStack-Color(Composite)�ɕϊ����ď�������Ǝ��R�ł͂Ȃ��Ȃ�B(�F�����ς��)
	*�eRGB�����ꂼ��ŏ�������镾�Q�Ɋ�����B
	*HSB�ɕϊ����āABrightness�̂ݏ������Ă��Ƃɖ߂��H
20230131
	RGB�摜��HSB�ϊ���Brightness��������ARGB�ɖ߂����@�Ŏ����B
	�܂��A�ȑO��Duplicate����̕ύX�ւ̑Ή�
	*crop����Ȃ��̂ŁA�V�������̃��\�b�h���g�p

20230607
	z���݂̂̏ꍇ�A��t���ɂ����Ă���ԈÂ��Ƃ���ɍ��킹��ق����s���̗ǂ��ꍇ������B
	�������������@�\���ǉ�����ׂ��B

@author		hwada
*/
import hw.autoenhance.CalEnhance;
import ij.*;
import ij.plugin.*;

public class AutoEnhance_ implements PlugIn {
	
	public void run( String arg ) {
		//���݂̃C���[�W�̎擾
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