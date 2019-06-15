package net.fabricmc.meta.web.models;

import com.google.gson.JsonObject;
import net.fabricmc.meta.utils.LoaderMeta;
import org.jetbrains.annotations.Nullable;

public class LoaderInfoV2 implements LoaderInfoBase {

	MavenBuildVersion loader;
	MavenVersion intermediary;

	@Nullable
	JsonObject launcherMeta;

	public LoaderInfoV2(MavenBuildVersion loader, MavenVersion intermediary) {
		this.loader = loader;
		this.intermediary = intermediary;
	}

	public LoaderInfoV2 populateMeta() {
		launcherMeta = LoaderMeta.getMeta(this);
		return this;
	}

	@Override
	public MavenBuildVersion getLoader() {
		return loader;
	}
}