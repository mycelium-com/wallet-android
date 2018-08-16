package com.mrd.bitlib.model.hdpath

import com.google.common.primitives.UnsignedInteger

class Bip49Purpose(parent: HdKeyPath, index: UnsignedInteger, hardened: Boolean) : Bip44Purpose(parent, index, hardened)