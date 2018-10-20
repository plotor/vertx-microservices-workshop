/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


export class PortfolioService {

  private closed = false;

  private readonly convCharCollection = coll => {
    const ret = [];
    for (let i = 0; i < coll.length; i++) {
      ret.push(String.fromCharCode(coll[i]));
    }
    return ret;
  }

  constructor (private eb: any, private address: string) {
  }

  getPortfolio(resultHandler: (err: any, result: any) => any) : void {
    if (closed) {
      throw new Error('Proxy is closed');
    }
    this.eb.send(this.address, {}, {"action":"getPortfolio"}, function(err, result) { resultHandler(err, result &&result.body); });
  }

  buy(amount: number, quote: Object, resultHandler: (err: any, result: any) => any) : void {
    if (closed) {
      throw new Error('Proxy is closed');
    }
    this.eb.send(this.address, {"amount": amount, "quote": quote}, {"action":"buy"}, function(err, result) { resultHandler(err, result &&result.body); });
  }

  sell(amount: number, quote: Object, resultHandler: (err: any, result: any) => any) : void {
    if (closed) {
      throw new Error('Proxy is closed');
    }
    this.eb.send(this.address, {"amount": amount, "quote": quote}, {"action":"sell"}, function(err, result) { resultHandler(err, result &&result.body); });
  }

  evaluate(resultHandler: (err: any, result: any) => any) : void {
    if (closed) {
      throw new Error('Proxy is closed');
    }
    this.eb.send(this.address, {}, {"action":"evaluate"}, function(err, result) { resultHandler(err, result &&result.body); });
  }

}