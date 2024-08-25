/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.example

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole

open class ExampleBenchmark {
//    Benchmark                         Mode  Cnt           Score          Error  Units
//    MyBenchmark.stringBuilder        thrpt   25    58391106,551 ±   366886,625  ops/s
//    MyBenchmark.stringFormatter      thrpt   25    14675607,197 ±   541527,576  ops/s
//    MyBenchmark.stringInterpolation  thrpt   25  2584265385,875 ± 22243457,062  ops/s
//    MyBenchmark.stringSumming        thrpt   25  2618914532,227 ± 23836478,869  ops/s
//    MyBenchmark.stringSumming2       thrpt   25    14520541,311 ±   235540,082  ops/s


//    @Benchmark
//    fun stringInterpolation(blackhole: Blackhole){
//        val x = "hello ${1} ${2} ${3} ${4} ${5}"
//        blackhole.consume(x)
//    }
//
//    @Benchmark
//    fun stringSumming(blackhole: Blackhole){
//        val x = "hello " + 1 + " " + 2 + " " + 3 + " " + 4 + " " + 5
//        blackhole.consume(x)
//    }
//
//    @Benchmark
//    fun stringSumming2(blackhole: Blackhole){
//        var x = "hello "
//        x+= 1
//        x+= " "
//        x+= 2
//        x+= " "
//        x+= 3
//        x+= " "
//        x+= 4
//        x+= " "
//        x+= 5
//        blackhole.consume(x)
//    }
//
//    @Benchmark
//    fun stringBuilder(blackhole: Blackhole){
//        val x = buildString {
//            append("hello ")
//            append(1)
//            append(" ")
//            append(2)
//            append(" ")
//            append(3)
//            append(" ")
//            append(4)
//            append(" ")
//            append(5)
//        }
//        blackhole.consume(x)
//    }
//
//    @Benchmark
//    fun stringFormatter(blackhole: Blackhole){
//        val x = "hello {} {} {} {} {}".format(1, 2, 3, 4, 5)
//        blackhole.consume(x)
//    }
}
