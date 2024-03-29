/*
 * Copyright 2012 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
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
 * limitations under the License.
 */
package com.yy.androidlib.util.prettytime.units;

import com.yy.androidlib.util.prettytime.TimeUnit;
import com.yy.androidlib.util.prettytime.impl.ResourcesTimeUnit;


public class Millisecond extends ResourcesTimeUnit implements TimeUnit {

    public Millisecond() {
        setMillisPerUnit(1);
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "Millisecond";
    }

}
