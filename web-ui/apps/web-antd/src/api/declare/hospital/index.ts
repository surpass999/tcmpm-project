import { requestClient } from '#/api/request';

export namespace DeclareHospitalApi {
  /**
   * 医院信息
   */
  export interface Hospital {
    id?: number;
    /** 关联部门ID */
    deptId?: number;
    /** 部门名称 */
    deptName?: string;
    /** 医院编码 */
    hospitalCode?: string;
    /** 医院全称 */
    hospitalName: string;
    /** 医院简称 */
    shortName?: string;
    /** 统一社会信用代码 */
    unifiedSocialCreditCode?: string;
    /** 执业许可证登记号 */
    medicalLicenseNo?: string;
    /** 执业许可证有效期 */
    medicalLicenseExpire?: string;
    /** 项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型 */
    projectType?: number;
    /** 项目类型名称 */
    projectTypeName?: string;
    /** 项目类型全称（如"综合型医院"） */
    projectTypeTitle?: string;
    /** 医院等级 */
    hospitalLevel?: string;
    /** 医院类别 */
    hospitalCategory?: string;
    /** 省份编码 */
    provinceCode?: string;
    /** 省份名称 */
    provinceName?: string;
    /** 城市编码 */
    cityCode?: string;
    /** 城市名称 */
    cityName?: string;
    /** 区县编码 */
    districtCode?: string;
    /** 区县名称 */
    districtName?: string;
    /** 详细地址 */
    address?: string;
    /** 联系人 */
    contactPerson?: string;
    /** 联系电话 */
    contactPhone?: string;
    /** 邮箱 */
    contactEmail?: string;
    /** 官方网站 */
    website?: string;
    /** 编制床位数 */
    bedCount?: number;
    /** 在职职工人数 */
    employeeCount?: number;
    /** 状态：0-停用 1-启用 */
    status?: number;
    /** 创建时间 */
    createTime?: string;
    /** 标签名称（逗号分隔） */
    tagNames?: string;
  }

  /**
   * 医院分页请求
   */
  export interface PageReqVO {
    pageNo?: number;
    pageSize?: number;
    hospitalName?: string;
    shortName?: string;
    deptId?: number;
    provinceCode?: string;
    cityCode?: string;
    hospitalLevel?: string;
    hospitalCategory?: string;
    projectType?: number;
    status?: number;
    unifiedSocialCreditCode?: string;
  }
}

/**
 * 获取医院分页列表
 */
export async function getHospitalPage(params: DeclareHospitalApi.PageReqVO) {
  return requestClient.get<{ list: DeclareHospitalApi.Hospital[]; total: number }>(
    '/declare/hospital/page',
    { params },
  );
}

/**
 * 获取医院详情
 */
export async function getHospital(id: number) {
  return requestClient.get<DeclareHospitalApi.Hospital>(`/declare/hospital/get?id=${id}`);
}

/**
 * 根据编码获取医院详情
 */
export async function getHospitalByCode(hospitalCode: string) {
  return requestClient.get<DeclareHospitalApi.Hospital>(
    `/declare/hospital/get-by-code?hospitalCode=${hospitalCode}`
  );
}

/**
 * 根据部门ID获取医院详情
 */
export async function getHospitalByDeptId(deptId: number) {
  return requestClient.get<DeclareHospitalApi.Hospital>(
    `/declare/hospital/get-by-dept-id?deptId=${deptId}`
  );
}

/**
 * 创建医院
 */
export async function createHospital(data: DeclareHospitalApi.Hospital) {
  return requestClient.post<number>('/declare/hospital/create', data);
}

/**
 * 更新医院
 */
export async function updateHospital(data: DeclareHospitalApi.Hospital) {
  return requestClient.put('/declare/hospital/update', data);
}

/**
 * 删除医院
 */
export async function deleteHospital(id: number) {
  return requestClient.delete(`/declare/hospital/delete?id=${id}`);
}

/**
 * 获取医院列表（简表）
 */
export async function getSimpleHospitalList() {
  return requestClient.get<DeclareHospitalApi.Hospital[]>('/declare/hospital/simple-list');
}

/**
 * 获取医院列表（按省份）
 */
export async function getHospitalListByProvince(provinceCode: string) {
  return requestClient.get<DeclareHospitalApi.Hospital[]>(
    `/declare/hospital/list-by-province?provinceCode=${provinceCode}`
  );
}

/**
 * 项目类型选项
 */
export const PROJECT_TYPE_OPTIONS = [
  { label: '综合型', value: 1 },
  { label: '中医电子病历型', value: 2 },
  { label: '智慧中药房型', value: 3 },
  { label: '名老中医传承型', value: 4 },
  { label: '中医临床科研型', value: 5 },
  { label: '中医智慧医共体型', value: 6 },
];

/**
 * 医院等级选项
 */
export const HOSPITAL_LEVEL_OPTIONS = [
  { label: '三级甲等', value: '三级甲等' },
  { label: '三级乙等', value: '三级乙等' },
  { label: '二级甲等', value: '二级甲等' },
  { label: '二级乙等', value: '二级乙等' },
  { label: '一级甲等', value: '一级甲等' },
  { label: '一级乙等', value: '一级乙等' },
  { label: '未定级', value: '未定级' },
];

/**
 * 医院类别选项
 */
export const HOSPITAL_CATEGORY_OPTIONS = [
  { label: '综合医院', value: '综合医院' },
  { label: '中医医院', value: '中医医院' },
  { label: '中西医结合医院', value: '中西医结合医院' },
  { label: '民族医医院', value: '民族医医院' },
  { label: '专科医院', value: '专科医院' },
];

/**
 * 下载医院导入模板
 */
export function importHospitalTemplate() {
  return requestClient.download('/declare/hospital/get-import-template');
}

/**
 * 导入医院
 */
export function importHospital(file: File, updateSupport: boolean) {
  return requestClient.upload('/declare/hospital/import', {
    file,
    updateSupport,
  });
}
