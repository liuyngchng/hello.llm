package rd.test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import org.yaml.snakeyaml.Yaml;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SysInit {

	private static final Logger LOGGER = LogManager.getLogger();

	private static Map<String, Object> initCfg = new HashMap<>();

	/**
	 * 初始化YAML配置，可以从cfg.yml文件读取配置
	 * 你可以复制cfg.yml.template并重写为你自己的cfg.yml
	 */
	public static Map<String, Object> initYmlCfg(String cfgFile) {
		if (!initCfg.isEmpty()) {
			LOGGER.info("cfg_already_inited_return_variable_initCfg: {}", initCfg);
			return initCfg;
		}

		// 检查配置文件是否存在
		if (!Files.exists(Paths.get(cfgFile))) {
			String info = String.format("配置文件 %s 不存在, 请根据根目录下的 %s.template 设置环境配置信息，完成后将文件重命名为 %s",
					cfgFile, cfgFile, cfgFile);
			LOGGER.info(info);
			System.exit(-2);
		}

		// 读取配置
		Yaml yaml = new Yaml();
		try (InputStream inputStream = Files.newInputStream(Paths.get(cfgFile))) {
			initCfg = yaml.load(inputStream);
			LOGGER.info("init_cfg_from_cfg_file: {}", initCfg);
		} catch (IOException e) {
			LOGGER.error("读取配置文件失败: {}", e.getMessage(), e);
			System.exit(-1);
		}

		return initCfg;
	}

	// 重载方法，使用默认配置文件名称
	public static Map<String, Object> initYmlCfg() {
		return initYmlCfg("config/cfg.yml");
	}

	public static void main(String[] args) {
		Map<String, Object> myCfg = initYmlCfg();
		LOGGER.info("cfg: {}", myCfg);

		Map<String, Object> myCfg1 = initYmlCfg();
		LOGGER.info("cfg: {}", myCfg1);
	}
}
