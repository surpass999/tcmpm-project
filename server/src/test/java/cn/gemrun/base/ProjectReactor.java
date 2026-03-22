package cn.gemrun.base;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.gemrun.base.framework.common.util.collection.SetUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.io.File.separator;

/**
 * 项目修改器，一键替换 Maven 的 groupId、artifactId，项目的 package 等
 * <p>
 * 通过修改 groupIdNew、artifactIdNew、projectBaseDirNew 三个变量
 *
 * @author 芋道源码
 */
@Slf4j
public class ProjectReactor {

    private static final String GROUP_ID = "cn.gemrun";
    private static final String ARTIFACT_ID = "base";
    private static final String PACKAGE_NAME = "cn.gemrun.base";
    private static final String TITLE = "项目管理系统";

    /**
     * 白名单文件，不进行重写，避免出问题
     */
    private static final Set<String> WHITE_FILE_TYPES = SetUtils.asSet("gif", "jpg", "svg", "png", // 图片
            "eot", "woff2", "ttf", "woff",  // 字体
            "xdb"); // IP 库

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        // 支持通过命令行参数指定要处理的源目录与目标目录，方便在不同执行上下文运行：
        // args[0] = projectBaseDir (可选)，args[1] = projectBaseDirNew (可选)
        String projectBaseDir = getProjectBaseDir();
        if (args != null && args.length >= 1 && StrUtil.isNotEmpty(args[0])) {
            projectBaseDir = args[0];
        }
        log.info("[main][原项目路劲改地址 ({})]", projectBaseDir);
        final String projectBaseDirFinal = projectBaseDir;

        // ========== 配置，需要你手动修改 ==========
        String groupIdNew = "cn.gemrun";
        String artifactIdNew = "base";
        String packageNameNew = "cn.gemrun.base";
        String titleNew = "中医医院管理系统";
        String projectBaseDirNew;
        if (args != null && args.length >= 2 && StrUtil.isNotEmpty(args[1])) {
            projectBaseDirNew = args[1];
        } else {
            projectBaseDirNew = projectBaseDir + "-new"; // 一键改名后，“新”项目所在的目录
        }
        log.info("[main][检测新项目目录 ({})是否存在]", projectBaseDirNew);
        if (FileUtil.exist(projectBaseDirNew)) {
            log.error("[main][新项目目录检测 ({})已存在，请更改新的目录！程序退出]", projectBaseDirNew);
            return;
        }
        // 如果新目录中存在 PACKAGE_NAME，ARTIFACT_ID 等关键字，路径会被替换，导致生成的文件不在预期目录
        if (StrUtil.containsAny(projectBaseDirNew, PACKAGE_NAME, ARTIFACT_ID, StrUtil.upperFirst(ARTIFACT_ID))) {
            log.error("[main][新项目目录 `projectBaseDirNew` 检测 ({}) 存在冲突名称「{}」或者「{}」，请更改新的目录！程序退出]",
                    projectBaseDirNew, PACKAGE_NAME, ARTIFACT_ID);
            return;
        }
        log.info("[main][完成新项目目录检测，新项目路径地址 ({})]", projectBaseDirNew);
        // 获得需要复制的文件
        log.info("[main][开始获得需要重写的文件，预计需要 10-20 秒]");
        Collection<File> files = listFiles(projectBaseDirFinal);
        log.info("[main][需要重写的文件数量：{}，预计需要 15-30 秒]", files.size());
        // 写入文件
        files.forEach(file -> {
            // 如果是白名单的文件类型，不进行重写，直接拷贝
            String fileType = getFileType(file);
            if (WHITE_FILE_TYPES.contains(fileType)) {
                copyFile(file, projectBaseDirFinal, projectBaseDirNew, packageNameNew, artifactIdNew);
                return;
            }
            // 如果非白名单的文件类型，重写内容，在生成文件
            String content = replaceFileContent(file, groupIdNew, artifactIdNew, packageNameNew, titleNew);
            writeFile(file, content, projectBaseDirFinal, projectBaseDirNew, packageNameNew, artifactIdNew);
        });
        log.info("[main][重写完成]共耗时：{} 秒", (System.currentTimeMillis() - start) / 1000);
    }

    private static String getProjectBaseDir() {
        String baseDir = System.getProperty("user.dir");
        if (StrUtil.isEmpty(baseDir)) {
            throw new NullPointerException("项目基础路径不存在");
        }
        return baseDir;
    }

    private static Collection<File> listFiles(String projectBaseDir) {
        Collection<File> files = FileUtil.loopFiles(projectBaseDir);
        // 移除 IDEA、Git 自身的文件、Node 编译出来的文件
        files = files.stream()
                .filter(file -> !file.getPath().contains(separator + "target" + separator)
                        && !file.getPath().contains(separator + "node_modules" + separator)
                        && !file.getPath().contains(separator + ".idea" + separator)
                        && !file.getPath().contains(separator + ".git" + separator)
                        && !file.getPath().contains(separator + "dist" + separator)
                        && !file.getPath().contains(".iml")
                        && !file.getPath().contains(".html.gz"))
                .collect(Collectors.toList());
        return files;
    }

    private static String replaceFileContent(File file, String groupIdNew,
                                             String artifactIdNew, String packageNameNew,
                                             String titleNew) {
        String content = FileUtil.readString(file, StandardCharsets.UTF_8);
        // 如果是白名单的文件类型，不进行重写
        String fileType = getFileType(file);
        if (WHITE_FILE_TYPES.contains(fileType)) {
            return content;
        }
        // 执行文件内容重写（先替换常规关键字）
        String replaced = content.replaceAll(GROUP_ID, groupIdNew)
                .replaceAll(PACKAGE_NAME, packageNameNew)
                .replaceAll(ARTIFACT_ID, artifactIdNew) // 必须放在最后替换，因为 ARTIFACT_ID 太短！
                .replaceAll(StrUtil.upperFirst(ARTIFACT_ID), StrUtil.upperFirst(artifactIdNew))
                .replaceAll(TITLE, titleNew);

        // 额外处理 pom.xml 中的 module 列表与模块 artifactId：
        // 有两种可能的前缀：原 ARTIFACT_ID（如 "base"）或新的 artifactIdNew（如 "base"），
        // 需要同时处理这两种情况，去掉 "<module>prefix-xxx</module>" / "<artifactId>prefix-xxx</artifactId>" 的 prefix。
        String oldPrefixPattern = Pattern.quote(ARTIFACT_ID);
        String newPrefixPattern = Pattern.quote(artifactIdNew);
        // 小写与首字母大写两种形式
        String oldUpperPattern = Pattern.quote(StrUtil.upperFirst(ARTIFACT_ID));
        String newUpperPattern = Pattern.quote(StrUtil.upperFirst(artifactIdNew));
        // 匹配并移除前缀（小写）
        replaced = replaced.replaceAll("<module>\\s*(?:" + oldPrefixPattern + "|" + newPrefixPattern + ")-([^<\\s]+)\\s*</module>", "<module>$1</module>");
        replaced = replaced.replaceAll("<artifactId>\\s*(?:" + oldPrefixPattern + "|" + newPrefixPattern + ")-([^<\\s]+)\\s*</artifactId>", "<artifactId>$1</artifactId>");
        // 匹配并移除前缀（首字母大写）
        replaced = replaced.replaceAll("<module>\\s*(?:" + oldUpperPattern + "|" + newUpperPattern + ")-([^<\\s]+)\\s*</module>", "<module>$1</module>");
        replaced = replaced.replaceAll("<artifactId>\\s*(?:" + oldUpperPattern + "|" + newUpperPattern + ")-([^<\\s]+)\\s*</artifactId>", "<artifactId>$1</artifactId>");

        return replaced;
    }

    private static void writeFile(File file, String fileContent, String projectBaseDir,
                                  String projectBaseDirNew, String packageNameNew, String artifactIdNew) {
        String newPath = buildNewFilePath(file, projectBaseDir, projectBaseDirNew, packageNameNew, artifactIdNew);
        FileUtil.writeUtf8String(fileContent, newPath);
    }

    private static void copyFile(File file, String projectBaseDir,
                                 String projectBaseDirNew, String packageNameNew, String artifactIdNew) {
        String newPath = buildNewFilePath(file, projectBaseDir, projectBaseDirNew, packageNameNew, artifactIdNew);
        FileUtil.copyFile(file, new File(newPath));
    }

    private static String buildNewFilePath(File file, String projectBaseDir,
                                           String projectBaseDirNew, String packageNameNew, String artifactIdNew) {
        String originalPath = file.getPath();
        // 先替换项目目录与 package 路径
        String interimPath = originalPath.replace(projectBaseDir, projectBaseDirNew)
                .replace(PACKAGE_NAME.replaceAll("\\.", Matcher.quoteReplacement(separator)),
                        packageNameNew.replaceAll("\\.", Matcher.quoteReplacement(separator)));

        // 按路径段处理：对于每个路径段，如果以 ARTIFACT_ID + "-" 开头，则去掉该前缀；
        // 如果路径段严格等于 ARTIFACT_ID，则替换为 artifactIdNew；同理处理首字母大写的情况。
        String[] segments = interimPath.split(Pattern.quote(separator));
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            if (seg == null || seg.length() == 0) {
                continue;
            }
            String prefix = ARTIFACT_ID + "-";
            if (seg.startsWith(prefix)) {
                segments[i] = seg.substring(prefix.length());
            } else if (seg.equals(ARTIFACT_ID)) {
                segments[i] = artifactIdNew;
            } else if (seg.equals(StrUtil.upperFirst(ARTIFACT_ID))) {
                segments[i] = StrUtil.upperFirst(artifactIdNew);
            }
        }

        String newPath = String.join(separator, segments);
        // 如果最后一段是文件名（包含扩展名），并且以原始类名前缀开头（或已替换后的新前缀），
        // 则同步替换文件名前缀，保证类名与文件名一致（处理首字母大写与小写两种情况）。
        if (segments.length > 0) {
            int lastIdx = segments.length - 1;
            String lastSeg = segments[lastIdx];
            if (lastSeg.contains(".")) {
                String oldUpper = StrUtil.upperFirst(ARTIFACT_ID);
                String newUpper = StrUtil.upperFirst(artifactIdNew);
                String oldLower = ARTIFACT_ID;
                String newLower = artifactIdNew;
                // 小写前缀文件名（极少见）
                if (lastSeg.startsWith(oldLower)) {
                    segments[lastIdx] = newLower + lastSeg.substring(oldLower.length());
                } else if (lastSeg.startsWith(oldUpper)) {
                    segments[lastIdx] = newUpper + lastSeg.substring(oldUpper.length());
                } else if (lastSeg.startsWith(newUpper)) {
                    // 如果内容已经替换成新类名，但文件名仍为旧形式，这里也尝试处理（幂等）
                    segments[lastIdx] = newUpper + lastSeg.substring(newUpper.length());
                } else if (lastSeg.startsWith(newLower)) {
                    segments[lastIdx] = newLower + lastSeg.substring(newLower.length());
                }
                newPath = String.join(separator, segments);
            }
        }
        // 保留原始路径是否以分隔符开始（例如绝对路径）
        if (originalPath.startsWith(separator) && !newPath.startsWith(separator)) {
            newPath = separator + newPath;
        }
        return newPath;
    }

    private static String getFileType(File file) {
        return file.length() > 0 ? FileTypeUtil.getType(file) : "";
    }

}
