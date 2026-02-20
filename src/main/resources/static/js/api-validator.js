


function validateApiResponse(endpointKey, response, headers = null) {

    const contract = window.APP_RUNTIME?.config?.api?.endpoints?.[endpointKey];

    if (!contract) {
        console.warn(`[API Validator] No contract found for endpoint: ${endpointKey}`);
        return true;
    }

    const errors = [];




    if (contract.response?.body) {
        const bodySchema = contract.response.body;


        if (bodySchema.type === 'array') {
            if (!Array.isArray(response)) {
                errors.push(`응답이 배열이어야 하지만 ${typeof response} 타입입니다.`);
            } else if (bodySchema.items) {

                if (response.length > 0) {
                    bodySchema.items.forEach(fieldDef => {
                        validateField(response[0], fieldDef, errors, '배열 첫 번째 아이템');
                    });
                }
            }
        }

        else if (bodySchema.fields) {
            bodySchema.fields.forEach(fieldDef => {
                validateField(response, fieldDef, errors);
            });
        }
    }




    if (contract.response?.headers && headers) {
        contract.response.headers.forEach(headerDef => {
            if (headerDef.required) {
                const headerValue = headers[headerDef.name.toLowerCase()];
                if (!headerValue) {
                    errors.push(`필수 헤더 누락: ${headerDef.name}`);
                }
            }
        });
    }




    if (errors.length > 0) {
        const expectedFormat = buildExpectedFormatMessage(contract);
        const howToFix = buildHowToFixMessage(endpointKey, errors, contract, response);


        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.error('⚠️ API 응답 형식 오류');
        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.error(`엔드포인트: ${contract.method} ${contract.url}`);
        console.error(`설명: ${contract.description || ''}`);
        console.error('');
        console.error('❌ 오류:');
        errors.forEach(e => console.error(`  • ${e}`));
        console.error('');
        console.error(expectedFormat);
        console.error('');
        console.error(howToFix);
        console.error('');
        console.error('실제 응답 데이터:', response);
        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');


        showApiValidationError(endpointKey, contract, errors);

        return false;
    }

    return true;
}


function validateField(data, fieldDef, errors, context = '') {
    const fieldName = fieldDef.name;
    const fieldValue = data[fieldName];
    const prefix = context ? `${context}.` : '';


    if (fieldDef.required && (fieldValue === undefined || fieldValue === null)) {
        errors.push(`필수 필드 누락: ${prefix}${fieldName}`);
        return;
    }


    if (fieldValue !== undefined && fieldValue !== null) {
        const actualType = Array.isArray(fieldValue) ? 'array' : typeof fieldValue;
        const expectedType = fieldDef.type;

        if (actualType !== expectedType) {
            errors.push(
                `${prefix}${fieldName} 타입 오류: ` +
                `${expectedType} 예상, ${actualType} 받음`
            );
        }
    }
}


function buildExpectedFormatMessage(contract) {
    const parts = [];


    if (contract.response?.body) {
        const bodySchema = contract.response.body;

        if (bodySchema.type === 'array') {
            parts.push('올바른 응답 형식 (배열):');
            parts.push('[');
            if (bodySchema.items) {
                parts.push('  {');
                bodySchema.items.forEach(field => {
                    const required = field.required ? ' (필수)' : '';
                    parts.push(`    "${field.name}": ${field.type}${required}`);
                });
                parts.push('  }');
            }
            parts.push(']');
        } else if (bodySchema.fields) {
            parts.push('올바른 응답 형식:');
            parts.push('{');
            bodySchema.fields.forEach(field => {
                const required = field.required ? ' (필수)' : '';
                parts.push(`  "${field.name}": ${field.type}${required}`);
            });
            parts.push('}');
        }
    }


    if (contract.response?.headers) {
        const requiredHeaders = contract.response.headers.filter(h => h.required);
        if (requiredHeaders.length > 0) {
            parts.push('\n필수 헤더:');
            requiredHeaders.forEach(header => {
                parts.push(`  ${header.name}: ${header.description || 'required'}`);
            });
        }
    }

    return parts.join('\n');
}


function getApiUrl(endpointKey) {
    const contract = window.APP_RUNTIME?.config?.api?.endpoints?.[endpointKey];
    return contract?.url || null;
}


function getApiMethod(endpointKey) {
    const contract = window.APP_RUNTIME?.config?.api?.endpoints?.[endpointKey];
    return contract?.method || 'GET';
}


function buildHowToFixMessage(endpointKey, errors, contract, response) {
    const parts = [];
    parts.push('📝 수정 방법 (아래 중 하나 선택):');
    parts.push('');

    const missingFields = [];
    const typeErrors = [];
    const headerErrors = [];

    errors.forEach(error => {
        if (error.includes('필수 필드 누락')) {
            const fieldName = error.split(': ')[1];
            missingFields.push(fieldName);
        } else if (error.includes('타입 오류')) {
            const match = error.match(/^(.+?) 타입 오류/);
            if (match) typeErrors.push(match[1]);
        } else if (error.includes('필수 헤더 누락')) {
            const headerName = error.split(': ')[1];
            headerErrors.push(headerName);
        }
    });

    parts.push('1️⃣ 백엔드 수정 (권장)');
    if (missingFields.length > 0) {
        parts.push('   Controller에서 응답에 다음 필드를 추가하세요:');
        missingFields.forEach(field => {
            const fieldDef = findFieldDefinition(contract, field);
            const example = getFieldExample(fieldDef);
            parts.push(`   response.put("${field}", ${example});`);
        });
    }
    if (typeErrors.length > 0) {
        parts.push('   다음 필드의 타입을 수정하세요:');
        typeErrors.forEach(field => {
            const fieldDef = findFieldDefinition(contract, field);
            parts.push(`   "${field}": ${fieldDef?.type} (으)로 변경`);
        });
    }
    if (headerErrors.length > 0) {
        parts.push('   응답 헤더를 추가하세요:');
        headerErrors.forEach(header => {
            if (header === 'Authorization') {
                parts.push(`   response.header("${header}", "Bearer " + token);`);
            } else {
                parts.push(`   response.header("${header}", "값");`);
            }
        });
    }
    parts.push('');

    parts.push('2️⃣ YML 수정 (client-api-config.yml)');
    parts.push(`   ${endpointKey} > response > body > fields`);
    if (missingFields.length > 0) {
        parts.push('   필수 필드를 optional로 변경하거나 제거:');
        missingFields.forEach(field => {
            parts.push(`   - name: ${field}`);
            parts.push(`     required: false  # ← 또는 이 필드를 삭제`);
        });
    }
    if (typeErrors.length > 0) {
        parts.push('   필드 타입을 실제 응답에 맞게 수정');
    }
    parts.push('');

    parts.push('3️⃣ 실제 응답 확인 (Console 탭)');
    parts.push('   개발자 도구 > Console 탭에서 실제 응답을 확인하세요.');
    parts.push('   실제 응답과 YML 스키마를 비교해보세요.');
    parts.push('');

    if (response && Object.keys(response).length > 0) {
        parts.push('💡 실제 응답 (처음 3개 필드):');
        const actualFields = Object.keys(response).slice(0, 3);
        actualFields.forEach(field => {
            const value = response[field];
            const type = Array.isArray(value) ? 'array' : typeof value;
            parts.push(`   "${field}": ${type}`);
        });
        if (Object.keys(response).length > 3) {
            parts.push(`   ... (총 ${Object.keys(response).length}개 필드)`);
        }
    }

    return parts.join('\n');
}


function findFieldDefinition(contract, fieldName) {
    if (!contract?.response?.body?.fields) return null;
    return contract.response.body.fields.find(f => f.name === fieldName);
}


function getFieldExample(fieldDef) {
    if (!fieldDef) return 'null';

    switch (fieldDef.type) {
        case 'string':
            return fieldDef.example ? `"${fieldDef.example}"` : '"example"';
        case 'number':
            return fieldDef.example || '0';
        case 'boolean':
            return fieldDef.example !== undefined ? fieldDef.example : 'true';
        case 'array':
            return '[]';
        case 'object':
            return '{}';
        default:
            return 'null';
    }
}


function showApiValidationError(endpointKey, contract, errors) {

    const errorCount = errors.length;
    const firstError = errors[0];

    const message =
        `⚠️ API 응답 형식 오류 (${errorCount}개)\n\n` +
        `엔드포인트: ${contract.method} ${contract.url}\n` +
        `첫 번째 오류: ${firstError}\n\n` +
        `🔍 자세한 정보는 개발자 도구의 Console 탭을 확인하세요.\n` +
        `(F12 키를 눌러 Console을 열 수 있습니다)`;

    // 기존 notification 시스템 사용
    if (typeof showNotification === 'function') {
        showNotification(
            `API 응답 오류 - Console 확인 필요 (${errorCount}개 오류)`,
            'error'
        );
    }

    // 추가로 alert도 표시 (간단한 버전)
    alert(message);
}
