package com.laomei.fatjar.common.boot.tool;

/**
 * Strategy interface used to determine the layout for a particular type of archive.
 * Layouts may additionally implement {@link CustomLoaderLayout} if they wish to write
 * custom loader classes.
 *
 * 移除了几个方法方法，事实上我们是需要构建 fat jar ， 并且都不需要是可执行的。
 *
 * @author Phillip Webb
 * @see Layouts
 * @see RepackagingLayout
 */
public interface Layout {

	/**
	 * Returns the destination path for a given library.
	 * @param libraryName the name of the library (excluding any path)
	 * @param scope the scope of the library
	 * @return the destination relative to the root of the archive (should end with '/')
	 * or {@code null} if the library should not be included.
	 */
	String getLibraryDestination(String libraryName, LibraryScope scope);

}
