package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.DownloadUtils;
import cn.keking.service.FileHandlerService;
import cn.keking.web.filter.BaseUrlFilter;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Created by kl on 2022/1/30.
 * Content :处理ofd文件,解决未知字体打开乱码的情况
 */
@Service
public class OfdFilePreviewImpl implements FilePreview {

    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;
    private final PictureFilePreviewImpl pictureFilePreview;
    private static final String FILE_DIR = ConfigConstants.getFileDir();

    public OfdFilePreviewImpl(FileHandlerService fileHandlerService, OtherFilePreviewImpl otherFilePreview,PictureFilePreviewImpl pictureFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.otherFilePreview = otherFilePreview;
        this.pictureFilePreview = pictureFilePreview;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        String fileName = fileAttribute.getName();
        String officePreviewType = fileAttribute.getOfficePreviewType();
        String baseUrl = BaseUrlFilter.getBaseUrl();
        String pdfName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + "ofd";//从pdf改成OFD
        String outFilePath = FILE_DIR + pdfName;
        if (OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType)) {
            //当文件不存在时，就去下载
            if (!fileHandlerService.listConvertedFiles().containsKey(pdfName) || !ConfigConstants.isCacheEnabled()) {
                ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
                if (response.isFailure()) {
                    return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
                }
                outFilePath = response.getContent();
                if (ConfigConstants.isCacheEnabled()) {
                    // 加入缓存
                    fileHandlerService.addConvertedFile(pdfName, fileHandlerService.getRelativePath(outFilePath));
                }
            }
            List<String> imageUrls = fileHandlerService.ofdtojpg(outFilePath, pdfName, baseUrl);
            if (imageUrls == null || imageUrls.size() < 1) {
                return otherFilePreview.notSupportedFile(model, fileAttribute, "OFD转图片异常，请联系管理员");
            }
            model.addAttribute("imgurls", imageUrls);
            model.addAttribute("currentUrl", imageUrls.get(0));
            if (OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType)) {
                return OFFICE_PICTURE_FILE_PREVIEW_PAGE;
            } else {
                return PICTURE_FILE_PREVIEW_PAGE;
            }
        } else {
            pictureFilePreview.filePreviewHandle(url,model,fileAttribute);
            return OFD_FILE_PREVIEW_PAGE;
        }
        //return PDF_FILE_PREVIEW_PAGE;
    }
}
