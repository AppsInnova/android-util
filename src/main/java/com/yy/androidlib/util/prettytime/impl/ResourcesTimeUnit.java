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
package com.yy.androidlib.util.prettytime.impl;

import com.yy.androidlib.util.prettytime.TimeUnit;


/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public abstract class ResourcesTimeUnit implements TimeUnit {
    private long maxQuantity = 0;
    private long millisPerUnit = 1;

    /**
     * Return the name of the resource bundle from which this unit's format should be loaded.
     */
    abstract protected String getResourceKeyPrefix();

    protected String getResourceBundleName() {
        return "com.yy.androidlib.util.prettytime.i18n.Resources";
    }

    @Override
    public long getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(long maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    @Override
    public long getMillisPerUnit() {
        return millisPerUnit;
    }

    public void setMillisPerUnit(long millisPerUnit) {
        this.millisPerUnit = millisPerUnit;
    }

}
