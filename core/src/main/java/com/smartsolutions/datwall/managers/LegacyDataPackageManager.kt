package com.smartsolutions.datwall.managers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LegacyDataPackageManager @Inject constructor(
    @ApplicationContext
    private val context: Context
): IDataPackageManager {
}