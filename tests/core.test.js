import test from 'node:test';
import assert from 'node:assert/strict';
import {validImei} from '../server.js';
test('valid IMEI passes Luhn checksum',()=>assert.equal(validImei('490154203237518'),true));
test('invalid checksum fails',()=>assert.equal(validImei('490154203237519'),false));
test('invalid length and characters fail',()=>{assert.equal(validImei('12345678901234'),false);assert.equal(validImei('49015420323751x'),false);});