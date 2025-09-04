package net.legacyfabric.meta.web.models;

import java.util.ArrayList;
import java.util.List;

public class MappingsDiff {
	public final List<Entry> classes = new ArrayList<>();
	public final List<ClassEntry> fields = new ArrayList<>();
	public final List<ClassEntry> methods = new ArrayList<>();

	public record Entry(String source, String target) {}
	public record ClassEntry(String owner, String source, String target, String sourceDesc, String targetDesc) {}
}
