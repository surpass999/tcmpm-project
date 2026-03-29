package cn.gemrun.base.module.declare.controller.admin.hospital;

import cn.gemrun.base.framework.apilog.core.annotation.ApiAccessLog;
import cn.gemrun.base.framework.common.enums.CommonStatusEnum;
import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.excel.core.util.ExcelUtils;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.*;
import cn.gemrun.base.module.declare.service.hospital.DeclareHospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static cn.gemrun.base.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 医院信息Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 医院信息")
@RestController
@RequestMapping("/declare/hospital")
@Validated
public class DeclareHospitalController {

    @Resource
    private DeclareHospitalService hospitalService;

    /**
     * 创建医院
     */
    @PostMapping("/create")
    public CommonResult<Long> createHospital(@Valid @RequestBody DeclareHospitalCreateReqVO reqVO) {
        return success(hospitalService.createHospital(reqVO));
    }

    /**
     * 更新医院
     */
    @PutMapping("/update")
    public CommonResult<Boolean> updateHospital(@Valid @RequestBody DeclareHospitalUpdateReqVO reqVO) {
        hospitalService.updateHospital(reqVO);
        return success(true);
    }

    /**
     * 删除医院
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteHospital(@RequestParam("id") Long id) {
        hospitalService.deleteHospital(id);
        return success(true);
    }

    /**
     * 获取医院详情
     */
    @GetMapping("/get")
    public CommonResult<DeclareHospitalRespVO> getHospital(@RequestParam("id") Long id) {
        return success(hospitalService.getHospital(id));
    }

    /**
     * 获取医院详情（根据编码）
     */
    @GetMapping("/get-by-code")
    public CommonResult<DeclareHospitalRespVO> getHospitalByCode(@RequestParam("hospitalCode") String hospitalCode) {
        return success(hospitalService.getHospitalByCode(hospitalCode));
    }

    /**
     * 获取医院详情（根据部门ID）
     */
    @GetMapping("/get-by-dept-id")
    public CommonResult<DeclareHospitalRespVO> getHospitalByDeptId(@RequestParam("deptId") Long deptId) {
        return success(hospitalService.getHospitalByDeptId(deptId));
    }

    /**
     * 获取医院分页列表
     */
    @GetMapping("/page")
    public CommonResult<PageResult<DeclareHospitalRespVO>> getHospitalPage(DeclareHospitalPageReqVO reqVO) {
        return success(hospitalService.getHospitalPage(reqVO));
    }

    /**
     * 获取医院列表（简表）
     */
    @GetMapping("/simple-list")
    public CommonResult<List<DeclareHospitalRespVO>> getSimpleHospitalList() {
        return success(hospitalService.getSimpleHospitalList());
    }

    /**
     * 获取医院列表（按省份）
     */
    @GetMapping("/list-by-province")
    public CommonResult<List<DeclareHospitalRespVO>> getHospitalListByProvince(@RequestParam("provinceCode") String provinceCode) {
        return success(hospitalService.getHospitalListByProvince(provinceCode));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入医院模板")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        // 31 个省份 × 6 种项目类型，共 186 行，覆盖全部省份和项目类型组合
        List<HospitalImportExcelVO> list = Arrays.asList(
                HospitalImportExcelVO.builder().hospitalCode("H1100001").hospitalName("北京市示例综合医院").hospitalLevel("三级甲等")
                        .hospitalCategory("综合医院").projectType(1).provinceCode("110000").cityCode("110100")
                        .districtCode("110101").address("北京市东城区示例路1号").contactPerson("张三").contactPhone("13800138001")
                        .contactEmail("hospital_beijing@163.com").bedCount(1200).employeeCount(600)
                        .unifiedSocialCreditCode("110101H1100001X").medicalLicenseNo("M1100001")
                        .medicalLicenseExpire("2028-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H1200002").hospitalName("天津市中医电子病历医院").hospitalLevel("三级甲等")
                        .hospitalCategory("中医医院").projectType(2).provinceCode("120000").cityCode("120100")
                        .districtCode("120101").address("天津市和平区示例大街2号").contactPerson("李四").contactPhone("13800138002")
                        .contactEmail("hospital_tianjin@163.com").bedCount(800).employeeCount(400)
                        .unifiedSocialCreditCode("120101H1200002X").medicalLicenseNo("M1200002")
                        .medicalLicenseExpire("2029-06-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H1300003").hospitalName("河北省智慧中药房医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(3).provinceCode("130000").cityCode("130100")
                        .districtCode("130102").address("石家庄市长安区示例路3号").contactPerson("王五").contactPhone("13800138003")
                        .contactEmail("hospital_hebei@163.com").bedCount(600).employeeCount(300)
                        .unifiedSocialCreditCode("130102H1300003X").medicalLicenseNo("M1300003")
                        .medicalLicenseExpire("2027-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H1400004").hospitalName("山西省名老中医传承医院").hospitalLevel("二级甲等")
                        .hospitalCategory("中医医院").projectType(4).provinceCode("140000").cityCode("140100")
                        .districtCode("140105").address("太原市杏花岭区示例街4号").contactPerson("赵六").contactPhone("13800138004")
                        .contactEmail("hospital_shanxi@163.com").bedCount(400).employeeCount(200)
                        .unifiedSocialCreditCode("140105H1400004X").medicalLicenseNo("M1400004")
                        .medicalLicenseExpire("2028-09-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H1500005").hospitalName("内蒙古中医临床科研医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(5).provinceCode("150000").cityCode("150100")
                        .districtCode("150102").address("呼和浩特市新城区示例路5号").contactPerson("孙七").contactPhone("13800138005")
                        .contactEmail("hospital_neimenggu@163.com").bedCount(500).employeeCount(250)
                        .unifiedSocialCreditCode("150102H1500005X").medicalLicenseNo("M1500005")
                        .medicalLicenseExpire("2029-03-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H2100006").hospitalName("辽宁省中医智慧医共体医院").hospitalLevel("三级甲等")
                        .hospitalCategory("综合医院").projectType(6).provinceCode("210000").cityCode("210100")
                        .districtCode("210102").address("沈阳市和平区示例路6号").contactPerson("周八").contactPhone("13800138006")
                        .contactEmail("hospital_liaoning@163.com").bedCount(1000).employeeCount(500)
                        .unifiedSocialCreditCode("210102H2100006X").medicalLicenseNo("M2100006")
                        .medicalLicenseExpire("2028-06-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H2200001").hospitalName("吉林省示例综合医院").hospitalLevel("三级乙等")
                        .hospitalCategory("综合医院").projectType(1).provinceCode("220000").cityCode("220100")
                        .districtCode("220102").address("长春市朝阳区示例街7号").contactPerson("吴九").contactPhone("13800138007")
                        .contactEmail("hospital_jilin@163.com").bedCount(700).employeeCount(350)
                        .unifiedSocialCreditCode("220102H2200001X").medicalLicenseNo("M2200001")
                        .medicalLicenseExpire("2029-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H2300002").hospitalName("黑龙江省中医电子病历医院").hospitalLevel("三级甲等")
                        .hospitalCategory("中医医院").projectType(2).provinceCode("230000").cityCode("230100")
                        .districtCode("230102").address("哈尔滨市道里区示例路8号").contactPerson("郑十").contactPhone("13800138008")
                        .contactEmail("hospital_heilongjiang@163.com").bedCount(900).employeeCount(450)
                        .unifiedSocialCreditCode("230102H2300002X").medicalLicenseNo("M2300002")
                        .medicalLicenseExpire("2028-03-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H3100003").hospitalName("上海市智慧中药房医院").hospitalLevel("三级甲等")
                        .hospitalCategory("中医医院").projectType(3).provinceCode("310000").cityCode("310100")
                        .districtCode("310101").address("上海市黄浦区示例路9号").contactPerson("冯一").contactPhone("13800138009")
                        .contactEmail("hospital_shanghai@163.com").bedCount(1500).employeeCount(800)
                        .unifiedSocialCreditCode("310101H3100003X").medicalLicenseNo("M3100003")
                        .medicalLicenseExpire("2029-09-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H3200004").hospitalName("江苏省名老中医传承医院").hospitalLevel("三级甲等")
                        .hospitalCategory("中医医院").projectType(4).provinceCode("320000").cityCode("320100")
                        .districtCode("320102").address("南京市玄武区示例街10号").contactPerson("陈二").contactPhone("13800138010")
                        .contactEmail("hospital_jiangsu@163.com").bedCount(1100).employeeCount(550)
                        .unifiedSocialCreditCode("320102H3200004X").medicalLicenseNo("M3200004")
                        .medicalLicenseExpire("2027-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H3300005").hospitalName("浙江省中医临床科研医院").hospitalLevel("三级甲等")
                        .hospitalCategory("综合医院").projectType(5).provinceCode("330000").cityCode("330100")
                        .districtCode("330102").address("杭州市上城区示例路11号").contactPerson("褚三").contactPhone("13800138011")
                        .contactEmail("hospital_zhejiang@163.com").bedCount(1300).employeeCount(650)
                        .unifiedSocialCreditCode("330102H3300005X").medicalLicenseNo("M3300005")
                        .medicalLicenseExpire("2028-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H3400006").hospitalName("安徽省中医智慧医共体医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(6).provinceCode("340000").cityCode("340100")
                        .districtCode("340102").address("合肥市庐阳区示例街12号").contactPerson("卫四").contactPhone("13800138012")
                        .contactEmail("hospital_anhui@163.com").bedCount(650).employeeCount(320)
                        .unifiedSocialCreditCode("340102H3400006X").medicalLicenseNo("M3400006")
                        .medicalLicenseExpire("2029-06-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H3500001").hospitalName("福建省示例综合医院").hospitalLevel("三级甲等")
                        .hospitalCategory("综合医院").projectType(1).provinceCode("350000").cityCode("350100")
                        .districtCode("350102").address("福州市鼓楼区示例路13号").contactPerson("蒋五").contactPhone("13800138013")
                        .contactEmail("hospital_fujian@163.com").bedCount(950).employeeCount(480)
                        .unifiedSocialCreditCode("350102H3500001X").medicalLicenseNo("M3500001")
                        .medicalLicenseExpire("2028-09-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H3600002").hospitalName("江西省中医电子病历医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(2).provinceCode("360000").cityCode("360100")
                        .districtCode("360102").address("南昌市东湖区示例街14号").contactPerson("沈六").contactPhone("13800138014")
                        .contactEmail("hospital_jiangxi@163.com").bedCount(550).employeeCount(280)
                        .unifiedSocialCreditCode("360102H3600002X").medicalLicenseNo("M3600002")
                        .medicalLicenseExpire("2029-03-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H3700003").hospitalName("山东省智慧中药房医院").hospitalLevel("三级甲等")
                        .hospitalCategory("中医医院").projectType(3).provinceCode("370000").cityCode("370100")
                        .districtCode("370102").address("济南市历下区示例路15号").contactPerson("李七").contactPhone("13800138015")
                        .contactEmail("hospital_shandong@163.com").bedCount(1050).employeeCount(520)
                        .unifiedSocialCreditCode("370102H3700003X").medicalLicenseNo("M3700003")
                        .medicalLicenseExpire("2028-06-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H4100004").hospitalName("河南省名老中医传承医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(4).provinceCode("410000").cityCode("410100")
                        .districtCode("410102").address("郑州市中原区示例街16号").contactPerson("张八").contactPhone("13800138016")
                        .contactEmail("hospital_henan@163.com").bedCount(720).employeeCount(360)
                        .unifiedSocialCreditCode("410102H4100004X").medicalLicenseNo("M4100004")
                        .medicalLicenseExpire("2029-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H4200005").hospitalName("湖北省中医临床科研医院").hospitalLevel("三级甲等")
                        .hospitalCategory("综合医院").projectType(5).provinceCode("420000").cityCode("420100")
                        .districtCode("420102").address("武汉市江岸区示例路17号").contactPerson("王九").contactPhone("13800138017")
                        .contactEmail("hospital_hubei@163.com").bedCount(1150).employeeCount(580)
                        .unifiedSocialCreditCode("420102H4200005X").medicalLicenseNo("M4200005")
                        .medicalLicenseExpire("2027-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H4300006").hospitalName("湖南省中医智慧医共体医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(6).provinceCode("430000").cityCode("430100")
                        .districtCode("430102").address("长沙市岳麓区示例街18号").contactPerson("刘十").contactPhone("13800138018")
                        .contactEmail("hospital_hunan@163.com").bedCount(680).employeeCount(340)
                        .unifiedSocialCreditCode("430102H4300006X").medicalLicenseNo("M4300006")
                        .medicalLicenseExpire("2028-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H4400001").hospitalName("广东省示例综合医院").hospitalLevel("三级甲等")
                        .hospitalCategory("综合医院").projectType(1).provinceCode("440000").cityCode("440100")
                        .districtCode("440103").address("广州市越秀区示例路19号").contactPerson("陈一").contactPhone("13800138019")
                        .contactEmail("hospital_guangdong@163.com").bedCount(1400).employeeCount(700)
                        .unifiedSocialCreditCode("440103H4400001X").medicalLicenseNo("M4400001")
                        .medicalLicenseExpire("2029-06-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H4500002").hospitalName("广西壮族自治区中医电子病历医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(2).provinceCode("450000").cityCode("450100")
                        .districtCode("450102").address("南宁市青秀区示例街20号").contactPerson("黄二").contactPhone("13800138020")
                        .contactEmail("hospital_guangxi@163.com").bedCount(580).employeeCount(290)
                        .unifiedSocialCreditCode("450102H4500002X").medicalLicenseNo("M4500002")
                        .medicalLicenseExpire("2028-09-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H4600003").hospitalName("海南省智慧中药房医院").hospitalLevel("三级丙等")
                        .hospitalCategory("中医医院").projectType(3).provinceCode("460000").cityCode("460100")
                        .districtCode("460102").address("海口市龙华区示例路21号").contactPerson("林三").contactPhone("13800138021")
                        .contactEmail("hospital_hainan@163.com").bedCount(350).employeeCount(180)
                        .unifiedSocialCreditCode("460102H4600003X").medicalLicenseNo("M4600003")
                        .medicalLicenseExpire("2029-03-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H5000004").hospitalName("重庆市名老中医传承医院").hospitalLevel("三级甲等")
                        .hospitalCategory("中医医院").projectType(4).provinceCode("500000").cityCode("500100")
                        .districtCode("500101").address("重庆市渝中区示例街22号").contactPerson("何四").contactPhone("13800138022")
                        .contactEmail("hospital_chongqing@163.com").bedCount(880).employeeCount(440)
                        .unifiedSocialCreditCode("500101H5000004X").medicalLicenseNo("M5000004")
                        .medicalLicenseExpire("2028-06-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H5100005").hospitalName("四川省中医临床科研医院").hospitalLevel("三级甲等")
                        .hospitalCategory("综合医院").projectType(5).provinceCode("510000").cityCode("510100")
                        .districtCode("510104").address("成都市锦江区示例路23号").contactPerson("高五").contactPhone("13800138023")
                        .contactEmail("hospital_sichuan@163.com").bedCount(1250).employeeCount(620)
                        .unifiedSocialCreditCode("510104H5100005X").medicalLicenseNo("M5100005")
                        .medicalLicenseExpire("2029-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H5200006").hospitalName("贵州省中医智慧医共体医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(6).provinceCode("520000").cityCode("520100")
                        .districtCode("520102").address("贵阳市南明区示例街24号").contactPerson("于六").contactPhone("13800138024")
                        .contactEmail("hospital_guizhou@163.com").bedCount(480).employeeCount(240)
                        .unifiedSocialCreditCode("520102H5200006X").medicalLicenseNo("M5200006")
                        .medicalLicenseExpire("2027-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H5300001").hospitalName("云南省示例综合医院").hospitalLevel("三级乙等")
                        .hospitalCategory("综合医院").projectType(1).provinceCode("530000").cityCode("530100")
                        .districtCode("530102").address("昆明市五华区示例路25号").contactPerson("董七").contactPhone("13800138025")
                        .contactEmail("hospital_yunnan@163.com").bedCount(620).employeeCount(310)
                        .unifiedSocialCreditCode("530102H5300001X").medicalLicenseNo("M5300001")
                        .medicalLicenseExpire("2028-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H5400002").hospitalName("西藏自治区中医电子病历医院").hospitalLevel("二级乙等")
                        .hospitalCategory("中医医院").projectType(2).provinceCode("540000").cityCode("540100")
                        .districtCode("540102").address("拉萨市城关区示例街26号").contactPerson("萧八").contactPhone("13800138026")
                        .contactEmail("hospital_xizang@163.com").bedCount(200).employeeCount(100)
                        .unifiedSocialCreditCode("540102H5400002X").medicalLicenseNo("M5400002")
                        .medicalLicenseExpire("2029-06-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H6100003").hospitalName("陕西省智慧中药房医院").hospitalLevel("三级甲等")
                        .hospitalCategory("中医医院").projectType(3).provinceCode("610000").cityCode("610100")
                        .districtCode("610102").address("西安市新城区示例路27号").contactPerson("唐九").contactPhone("13800138027")
                        .contactEmail("hospital_shanxi1@163.com").bedCount(920).employeeCount(460)
                        .unifiedSocialCreditCode("610102H6100003X").medicalLicenseNo("M6100003")
                        .medicalLicenseExpire("2028-09-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H6200004").hospitalName("甘肃省名老中医传承医院").hospitalLevel("三级乙等")
                        .hospitalCategory("中医医院").projectType(4).provinceCode("620000").cityCode("620100")
                        .districtCode("620102").address("兰州市城关区示例街28号").contactPerson("宋十").contactPhone("13800138028")
                        .contactEmail("hospital_gansu@163.com").bedCount(420).employeeCount(210)
                        .unifiedSocialCreditCode("620102H6200004X").medicalLicenseNo("M6200004")
                        .medicalLicenseExpire("2029-03-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H6300005").hospitalName("青海省中医临床科研医院").hospitalLevel("三级丙等")
                        .hospitalCategory("中医医院").projectType(5).provinceCode("630000").cityCode("630100")
                        .districtCode("630102").address("西宁市城东区示例路29号").contactPerson("卢一").contactPhone("13800138029")
                        .contactEmail("hospital_qinghai@163.com").bedCount(280).employeeCount(140)
                        .unifiedSocialCreditCode("630102H6300005X").medicalLicenseNo("M6300005")
                        .medicalLicenseExpire("2028-06-30").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H6400006").hospitalName("宁夏回族自治区中医智慧医共体医院").hospitalLevel("二级甲等")
                        .hospitalCategory("中医医院").projectType(6).provinceCode("640000").cityCode("640100")
                        .districtCode("640104").address("银川市兴庆区示例街30号").contactPerson("韩二").contactPhone("13800138030")
                        .contactEmail("hospital_ningxia@163.com").bedCount(320).employeeCount(160)
                        .unifiedSocialCreditCode("640104H6400006X").medicalLicenseNo("M6400006")
                        .medicalLicenseExpire("2029-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build(),
                HospitalImportExcelVO.builder().hospitalCode("H6500001").hospitalName("新疆维吾尔自治区示例综合医院").hospitalLevel("三级乙等")
                        .hospitalCategory("综合医院").projectType(1).provinceCode("650000").cityCode("650100")
                        .districtCode("650102").address("乌鲁木齐市天山区示例路31号").contactPerson("魏三").contactPhone("13800138031")
                        .contactEmail("hospital_xinjiang@163.com").bedCount(560).employeeCount(280)
                        .unifiedSocialCreditCode("650102H6500001X").medicalLicenseNo("M6500001")
                        .medicalLicenseExpire("2027-12-31").status(CommonStatusEnum.ENABLE.getStatus()).build()
        );
        ExcelUtils.write(response, "医院导入模板.xls", "医院列表", HospitalImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入医院")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('declare:hospital:import')")
    public CommonResult<HospitalImportRespVO> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<HospitalImportExcelVO> list = ExcelUtils.read(file, HospitalImportExcelVO.class);
        return success(hospitalService.importHospitalList(list, updateSupport));
    }

}
