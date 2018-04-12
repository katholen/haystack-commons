/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.expedia.www.haystack.commons.entities.encodings

import java.nio.charset.StandardCharsets

import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding

class Base64Encoding extends Encoding {
  def encode(value: String): String = {
    BaseEncoding.base64().withPadChar('_').encode(value.getBytes(Charsets.UTF_8))
  }

  def decode(value: String): String = {
    new String(BaseEncoding.base64().withPadChar('_').decode(value), StandardCharsets.UTF_8)
  }
}