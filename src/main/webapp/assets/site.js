(() => {
    const context = document.querySelector('meta[name="app-context"]').content;
    let token = document.querySelector('meta[name="csrf-token"]').content;

    function show(message, at = document.querySelector('#notice')) {
        at.textContent = message;
        at.hidden = false;
        if (at.id === 'notice') at.scrollIntoView({ block: 'nearest' });
    }

    for (const time of document.querySelectorAll('time[datetime]')) {
        const date = new Date(time.dateTime);
        if (!Number.isNaN(date.getTime())) {
            time.textContent = new Intl.DateTimeFormat('zh-CN', {
                timeZone: 'Asia/Shanghai', month: '2-digit', day: '2-digit',
                hour: '2-digit', minute: '2-digit', hour12: false
            }).format(date);
        }
    }

    for (const input of document.querySelectorAll('input[type="datetime-local"][data-utc]')) {
        const date = new Date(input.dataset.utc);
        if (!Number.isNaN(date.getTime())) input.value = new Date(date.getTime() + 8 * 3600000).toISOString().slice(0, 16);
    }

    for (const button of document.querySelectorAll('[data-copy-invite]')) {
        const field = document.getElementById(button.dataset.copyInvite);
        const status = button.closest('section').querySelector('[data-copy-invite-status]');
        // 浏览器限制剪贴板时选中邀请码，方便手动复制。
        button.addEventListener('click', async () => {
            status.hidden = false;
            try {
                await navigator.clipboard.writeText(field.value);
                status.textContent = '已复制邀请码。';
            } catch {
                field.focus();
                field.select();
                status.textContent = '邀请码已选中，请手动复制。';
            }
        });
    }

    for (const rubric of document.querySelectorAll('[data-rubric]')) {
        const list = rubric.querySelector('[data-rubric-list]');
        const template = rubric.querySelector('[data-rubric-template]');
        const add = rubric.querySelector('[data-rubric-add]');
        const total = rubric.querySelector('[data-rubric-total]');
        const ids = new Set([...list.children].map(row => row.dataset.rubricId));
        let nextId = 0;

        function refresh() {
            const sum = [...list.querySelectorAll('[data-rubric-weight]')]
                .reduce((weight, field) => weight + Math.round(Number(field.value) * 100), 0);
            total.textContent = '当前权重 ' + (sum / 100).toFixed(2) + '%，合计须为 100%。';
            add.disabled = list.children.length >= 12;
            for (const remove of list.querySelectorAll('[data-rubric-remove]')) {
                remove.disabled = list.children.length <= 1;
            }
        }

        add.addEventListener('click', () => {
            const row = template.content.firstElementChild.cloneNode(true);
            let id;
            do { id = 'criterion-' + ++nextId; } while (ids.has(id));
            ids.add(id);
            row.dataset.rubricId = id;
            list.append(row);
            row.querySelector('[data-rubric-label]').focus();
            refresh();
        });
        list.addEventListener('click', event => {
            const remove = event.target.closest('[data-rubric-remove]');
            if (remove && list.children.length > 1) {
                remove.closest('[data-rubric-id]').remove();
                add.focus();
                refresh();
            }
        });
        list.addEventListener('input', refresh);
        refresh();
    }

    for (const form of document.querySelectorAll('[data-student-verification]')) {
        const decision = form.elements.namedItem('decision');
        const number = form.elements.namedItem('studentNo');

        // 正式学号仅在核验通过时参与校验和提交。
        function updateStudentNumber() {
            number.required = decision.value === 'APPROVE';
            number.disabled = !number.required;
        }

        decision.addEventListener('change', updateStudentNumber);
        updateStudentNumber();
    }

    for (const form of document.querySelectorAll('form[data-api]')) {
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (form.dataset.busy || !form.reportValidity()) return;
            const submit = event.submitter || form.querySelector('button');
            const message = form.querySelector('[data-message]') || document.querySelector('#notice');
            const data = new FormData(form);
            if (submit?.name) data.set(submit.name, submit.value);
            if (submit?.dataset.confirm && !window.confirm(submit.dataset.confirm)) return;
            const method = submit?.dataset.method || form.dataset.method || 'POST';
            const path = submit?.dataset.api || form.dataset.api;
            form.dataset.busy = 'true';
            if (submit) submit.disabled = true;
            message.hidden = true;
            try {
                const options = { method, headers: { 'X-CSRF-Token': token } };
                if (form.enctype === 'multipart/form-data') {
                    options.body = data;
                } else if (method !== 'DELETE') {
                    const values = Object.fromEntries(data);
                    for (const element of form.querySelectorAll('[data-array]')) {
                        values[element.name] = data.getAll(element.name);
                    }
                    for (const element of form.querySelectorAll('[data-utc]')) {
                        if (element.value) values[element.name] = element.value + ':00+08:00';
                    }
                    const rubricRows = [...form.querySelectorAll('[data-rubric-id]')];
                    if (rubricRows.length) {
                        values.scoringRubric = rubricRows.map(row => ({
                            id: row.dataset.rubricId, label: row.querySelector('[data-rubric-label]').value,
                            weight: row.querySelector('[data-rubric-weight]').value
                        }));
                    }
                    const scoreInputs = [...form.querySelectorAll('[data-score]')];
                    if (scoreInputs.length) {
                        values.criterionScores = Object.fromEntries(scoreInputs.map(input => [input.dataset.score, input.value]));
                    }
                    options.headers['Content-Type'] = 'application/json';
                    options.body = JSON.stringify(values);
                }
                const response = await fetch(context + '/api/v1' + path, options);
                const result = await response.json().catch(() => ({}));
                if (!response.ok) throw new Error(result.message || '操作未完成，请稍后重试。');
                if (result.csrfToken) token = result.csrfToken;
                const next = submit?.dataset.next ?? form.dataset.next;
                if (next === '@home') location.assign(context + result.home);
                else if (next) location.assign(context + next.replace('{id}', result.id ?? ''));
                else location.reload();
            } catch (error) {
                show(error.message, message);
            } finally {
                delete form.dataset.busy;
                if (submit) submit.disabled = false;
            }
        });
    }

    for (const frame of document.querySelectorAll('iframe[data-pdf]')) {
        const fallback = frame.parentElement.querySelector('[data-pdf-message]');
        if (navigator.pdfViewerEnabled === false) {
            frame.hidden = true;
            fallback.hidden = false;
            continue;
        }
        fetch(context + '/api/v1' + frame.dataset.pdf).then(response => {
            if (!response.ok || !response.headers.get('Content-Type')?.startsWith('application/pdf')) throw new Error();
            return response.blob();
        }).then(blob => {
            const fileUrl = URL.createObjectURL(blob);
            frame.src = fileUrl;
            window.addEventListener('pagehide', () => URL.revokeObjectURL(fileUrl), { once: true });
        }).catch(() => {
            frame.hidden = true;
            fallback.hidden = false;
        });
    }

    for (const form of document.querySelectorAll('form[data-results]')) {
        const prefix = context + '/api/v1/admin/competitions/' + form.dataset.results + '/results';
        const message = form.querySelector('[data-result-message]');
        const preview = form.querySelector('[data-result-preview]');
        const entries = form.querySelector('[data-result-entries]');
        const confirm = form.querySelector('[data-result-confirm]');
        let prepared = null;
        let busy = false;

        async function send(path, payload) {
            const response = await fetch(prefix + path, {
                method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': token },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            if (!response.ok) throw new Error(result.message || '结果操作未完成。');
            return result;
        }

        function invalidate() {
            prepared = null;
            confirm.disabled = true;
            preview.hidden = true;
        }

        form.addEventListener('input', invalidate);
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (busy || !form.reportValidity()) return;
            invalidate();
            const data = new FormData(form);
            const current = data.get('currentPublicationId');
            const payload = {
                mode: current ? 'CORRECTION' : 'FIRST', noAwards: data.has('noAwards'),
                requestId: data.get('requestId'), awards: []
            };
            if (current) {
                payload.currentPublicationId = current;
                payload.reason = data.get('reason');
            }
            for (const row of form.querySelectorAll('[data-result-row]')) {
                const entry = { registrationId: row.dataset.resultRow };
                for (const field of row.querySelectorAll('[data-edit]')) {
                    if (field.disabled) continue;
                    if (field.dataset.edit === 'recipientNames') {
                        entry.recipientNames = field.value.split(/[、，,]/u).map(name => name.trim()).filter(Boolean);
                    } else if (field.type === 'checkbox') {
                        if (field.checked) entry[field.dataset.edit] = true;
                    } else if (field.value.trim() || field.dataset.edit === 'awardName') {
                        entry[field.dataset.edit] = field.value;
                    }
                }
                payload.awards.push(entry);
            }
            busy = true;
            const revision = JSON.stringify(payload);
            const fields = [...form.querySelectorAll('input, textarea, button[type="submit"]')];
            const enabled = fields.filter(field => !field.disabled);
            enabled.forEach(field => { field.disabled = true; });
            try {
                const result = await send('/preview', JSON.parse(revision));
                entries.replaceChildren(...result.items.map(item => {
                    const line = document.createElement('li');
                    const place = item.rankNo ? '第 ' + item.rankNo + ' 名 · ' + item.totalScoreText + ' 分' : '未排名';
                    line.textContent = [place, item.workTitle, item.entryDisplay, item.awardName,
                        (item.recipientNames || []).join('、')].filter(Boolean).join(' · ');
                    return line;
                }));
                preview.hidden = !result.items.length;
                if (result.canPublish) {
                    prepared = { ...payload, previewToken: result.previewToken };
                    confirm.disabled = false;
                    show('预览已通过。核对下方的公布内容后确认发布。', message);
                } else {
                    show(result.problems.join(' '), message);
                }
            } catch (error) {
                show(error.message, message);
            } finally {
                enabled.forEach(field => { field.disabled = false; });
                busy = false;
            }
        });

        confirm.addEventListener('click', async () => {
            if (busy || !prepared || !window.confirm('确认发布这份结果？')) return;
            busy = true;
            confirm.disabled = true;
            try {
                await send(prepared.mode === 'FIRST' ? '/publications' : '/corrections', prepared);
                location.reload();
            } catch (error) {
                invalidate();
                show(error.message, message);
            } finally {
                busy = false;
            }
        });
    }
})();
