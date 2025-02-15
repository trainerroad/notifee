"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const utils_1 = require("../../utils");
const validateIOSCommunicationInfoPerson_1 = __importDefault(require("./validateIOSCommunicationInfoPerson"));
function validateIOSCommunicationInfo(communicationInfo) {
    if (!(0, utils_1.isObject)(communicationInfo)) {
        throw new Error('expected an object.');
    }
    if (!(0, utils_1.isString)(communicationInfo.conversationId) ||
        communicationInfo.conversationId.length === 0) {
        throw new Error("'conversationId' expected a valid string value.");
    }
    if (!communicationInfo.sender || !(0, utils_1.isObject)(communicationInfo.sender)) {
        throw new Error("'sender' expected a valid object value.");
    }
    let sender;
    try {
        sender = (0, validateIOSCommunicationInfoPerson_1.default)(communicationInfo.sender);
    }
    catch (e) {
        throw new Error(`'sender' ${e.message}.`);
    }
    const out = {
        conversationId: communicationInfo.conversationId,
        sender,
    };
    if (communicationInfo.body) {
        if (!(0, utils_1.isString)(communicationInfo.body)) {
            throw new Error("'body' expected a valid string value.");
        }
        out.body = communicationInfo.body;
    }
    return out;
}
exports.default = validateIOSCommunicationInfo;
//# sourceMappingURL=validateIOSCommunicationInfo.js.map