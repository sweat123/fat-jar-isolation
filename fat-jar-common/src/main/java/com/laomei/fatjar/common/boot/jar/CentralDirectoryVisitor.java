package com.laomei.fatjar.common.boot.jar;

import com.laomei.fatjar.common.boot.data.RandomAccessData;

/**
 * Callback visitor triggered by {@link CentralDirectoryParser}.
 *
 * @author Phillip Webb
 */
interface CentralDirectoryVisitor {

	void visitStart(CentralDirectoryEndRecord endRecord,
            RandomAccessData centralDirectoryData);

	void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset);

	void visitEnd();

}
