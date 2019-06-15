package net.fabricmc.meta.web.models;

import com.google.gson.JsonObject;
import net.fabricmc.meta.utils.LoaderMeta;
import org.jetbrains.annotations.Nullable;

public class LoaderInfoV1 implements LoaderInfoBase {

	MavenBuildVersion loader;
	MavenBuildGameVersion mappings;

	@Nullable
	JsonObject launcherMeta;

	public LoaderInfoV1(MavenBuildVersion loader, MavenBuildGameVersion mappings) {
		this.loader = loader;
		this.mappings = mappings;
	}

	public LoaderInfoV1 populateMeta() {
		launcherMeta = LoaderMeta.getMeta(this);
		return this;
	}

	@Override
	public MavenBuildVersion getLoader() {
		return loader;
	}
}