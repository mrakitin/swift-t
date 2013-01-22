/*
 * Copyright 2013 University of Chicago and Argonne National Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 * Copyright [yyyy] [name of copyright owner]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License..
 */
package exm.stc.common.lang;

import java.util.ArrayList;
import java.util.List;

import exm.stc.common.lang.Var.DefType;

public class RefCounting {
  
  /**
   * @param t
   * @return true if type has read refcount to be managed
   */
  public static boolean hasReadRefcount(Var v) {
    if (Types.isScalarValue(v.type())) {
      return false;
    } else if (v.defType() == DefType.GLOBAL_CONST) {
      return false;
    }
    return true;
  }
  
  /**
   * Return true if writer count is tracked for type
   * @param t
   * @return
   */
  public static boolean hasWriteRefcount(Var v) {
    return Types.isArray(v.type()) && v.defType() != DefType.GLOBAL_CONST;
  }
  
  /**
   * Filter vars to include only variables where writers count is tracked
   * @param outputs
   * @return
   */
  public static List<Var> filterWriteRefcount(List<Var> vars) {
    assert(vars != null);
    List<Var> res = new ArrayList<Var>();
    for (Var var: vars) {
      if (hasWriteRefcount(var)) {
        res.add(var);
      }
    }
    return res;
  }
}
