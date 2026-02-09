package cn.gemrun.base.module.infra.convert.file;

import cn.gemrun.base.module.infra.controller.admin.file.vo.config.FileConfigSaveReqVO;
import cn.gemrun.base.module.infra.dal.dataobject.file.FileConfigDO;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-09T17:50:35+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 1.8.0_371 (Oracle Corporation)"
)
public class FileConfigConvertImpl implements FileConfigConvert {

    @Override
    public FileConfigDO convert(FileConfigSaveReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        FileConfigDO.FileConfigDOBuilder fileConfigDO = FileConfigDO.builder();

        fileConfigDO.id( bean.getId() );
        fileConfigDO.name( bean.getName() );
        fileConfigDO.storage( bean.getStorage() );
        fileConfigDO.remark( bean.getRemark() );

        return fileConfigDO.build();
    }
}
