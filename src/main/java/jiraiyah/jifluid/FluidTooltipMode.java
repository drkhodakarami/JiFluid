/***********************************************************************************
 * Copyright (c) 2024 Alireza Khodakarami (Jiraiyah)                               *
 * ------------------------------------------------------------------------------- *
 * MIT License                                                                     *
 * =============================================================================== *
 * Permission is hereby granted, free of charge, to any person obtaining a copy    *
 * of this software and associated documentation files (the "Software"), to deal   *
 * in the Software without restriction, including without limitation the rights    *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell       *
 * copies of the Software, and to permit persons to whom the Software is           *
 * furnished to do so, subject to the following conditions:                        *
 * ------------------------------------------------------------------------------- *
 * The above copyright notice and this permission notice shall be included in all  *
 * copies or substantial portions of the Software.                                 *
 * ------------------------------------------------------------------------------- *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR      *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,        *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE     *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER          *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,   *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE   *
 * SOFTWARE.                                                                       *
 ***********************************************************************************/

package jiraiyah.jifluid;

/**
 * Enum representing the different modes for displaying fluid tooltips.
 * This can be used to customize how fluid quantities are shown in the UI.
 */
public enum FluidTooltipMode
{
    /**
     * Display the amount of fluid using the Fabric modding platform's conventions.
     */
    SHOW_AMOUNT_FABRIC,

    /**
     * Display both the amount of fluid and its capacity using the Fabric modding platform's conventions.
     */
    SHOW_AMOUNT_AND_CAPACITY_FABRIC,

    /**
     * Display the amount of fluid in milli-buckets (mB).
     */
    SHOW_AMOUNT_MB,

    /**
     * Display both the amount of fluid and its capacity in milli-buckets (mB).
     */
    SHOW_AMOUNT_AND_CAPACITY_MB
}