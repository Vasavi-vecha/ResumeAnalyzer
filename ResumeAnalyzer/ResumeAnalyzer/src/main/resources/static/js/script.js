// ====================  LOADING OVERLAY FUNCTIONS ====================
function showLoading(message = 'Processing...', submessage = 'Please wait while we analyze your documents') {
    const overlay = document.getElementById('loading-overlay');
    const messageEl = document.getElementById('loading-message');
    const submessageEl = document.getElementById('loading-submessage');

    messageEl.textContent = message;
    submessageEl.textContent = submessage;
    overlay.classList.remove('d-none');
}

function hideLoading() {
    const overlay = document.getElementById('loading-overlay');
    overlay.classList.add('d-none');
}

// ==================== UPLOAD MODAL FUNCTIONS ====================
function showUploadModal(title = 'Document Uploading', message = 'Please wait while the document is being uploaded...', isSuccess = false) {
    console.log('showUploadModal called with:', title, isSuccess);
    const modal = document.getElementById('upload-modal');
    const titleEl = document.getElementById('upload-modal-title');
    const messageEl = document.getElementById('upload-modal-message');
    const spinner = document.getElementById('upload-spinner');
    const successIcon = document.getElementById('upload-success-icon');

    console.log('Modal element:', modal);
    console.log('Modal classes before:', modal?.className);

    titleEl.textContent = title;
    messageEl.textContent = message;

    if (isSuccess) {
        // Hide spinner, show success icon
        spinner.classList.add('d-none');
        successIcon.classList.remove('d-none');
        titleEl.classList.add('text-success');
    } else {
        // Show spinner, hide success icon
        spinner.classList.remove('d-none');
        successIcon.classList.add('d-none');
        titleEl.classList.remove('text-success');
    }

    modal.classList.remove('d-none');
    console.log('Modal classes after:', modal?.className);
    console.log('Modal should be visible now!');
}

function hideUploadModal() {
    const modal = document.getElementById('upload-modal');
    modal.classList.add('d-none');

    // Reset to default state
    const spinner = document.getElementById('upload-spinner');
    const successIcon = document.getElementById('upload-success-icon');
    const titleEl = document.getElementById('upload-modal-title');

    spinner.classList.remove('d-none');
    successIcon.classList.add('d-none');
    titleEl.classList.remove('text-success');
}

// ==================== DUPLICATE MODAL FUNCTIONS ====================
function showDuplicateModal(rfrName, resumeName) {
    const modal = document.getElementById('duplicate-modal');
    const rfrNameEl = document.getElementById('duplicate-rfr-name');
    const resumeNameEl = document.getElementById('duplicate-resume-name');

    rfrNameEl.textContent = rfrName;
    resumeNameEl.textContent = resumeName;
    modal.classList.remove('d-none');
}

function closeDuplicateModal() {
    const modal = document.getElementById('duplicate-modal');
    modal.classList.add('d-none');

    // Clear form fields after closing modal
    resetFileInput('rfr-file', 'rfr-file-name', '#single-tab .upload-box');
    resetFileInput('resume-file', 'resume-file-name', '#single-tab .upload-box');

    // Scroll to results section after closing modal
    document.getElementById('evaluations-section')?.scrollIntoView({ behavior: 'smooth' });
}

// ==================== SUCCESS NOTIFICATION ====================
function showSuccess(message = 'Evaluation completed successfully!') {
    const successSection = document.getElementById('success-section');
    const successText = document.getElementById('success-text');

    if (successSection && successText) {
        successText.textContent = message;
        successSection.classList.remove('d-none');
        successSection.classList.add('show');

        // Auto-hide after 5 seconds
        setTimeout(() => {
            hideSuccess();
        }, 5000);
    }
}

function hideSuccess() {
    const successSection = document.getElementById('success-section');
    if (successSection) {
        successSection.classList.remove('show');
        successSection.classList.add('d-none');
    }
}

// ==================== FORM RESET FUNCTIONS ====================
function resetFileInput(inputId, displayId, uploadBoxSelector) {
    console.log('resetFileInput called:', inputId, displayId, uploadBoxSelector);
    const input = document.getElementById(inputId);
    const display = document.getElementById(displayId);

    if (input) {
        input.value = '';
        console.log('Input cleared:', inputId);

        // Get the upload box that contains this input
        const uploadBox = input.closest('.upload-box');
        if (uploadBox) {
            uploadBox.classList.remove('file-selected');
            console.log('Upload box file-selected class removed for:', inputId);
        } else {
            console.log('Upload box NOT found for:', inputId);
        }
    } else {
        console.log('Input NOT found:', inputId);
    }

    if (display) {
        display.textContent = '';
        console.log('Display cleared:', displayId);
    } else {
        console.log('Display NOT found:', displayId);
    }
}

function resetForm(formId) {
    const form = document.getElementById(formId);
    if (form) form.reset();
}

// ==================== FILE SELECTION DISPLAY ====================
// File selection display with visual feedback and upload modal
document.getElementById('rfr-file')?.addEventListener('change', function(e) {
    console.log('RFR file change event triggered');
    const file = e.target.files[0];
    const fileNameDisplay = document.getElementById('rfr-file-name');
    const uploadBox = this.closest('.upload-box');

    if (file) {
        const fileName = file.name;
        console.log('File selected:', fileName);

        // Show modal IMMEDIATELY - no delays!
        console.log('Showing modal now...');
        showUploadModal('Uploading Document...', `Please wait, "${fileName}" is being uploaded...`, false);

        // Brief processing delay to show buffer
        setTimeout(() => {
            console.log('Showing success...');
            // Show success
            showUploadModal('Upload Complete!', `"${fileName}" has been successfully uploaded and is ready to use.`, true);

            // Update UI
            fileNameDisplay.textContent = '✓ ' + fileName;
            uploadBox.classList.add('file-selected');

            // Auto-close modal
            setTimeout(() => {
                console.log('Hiding modal...');
                hideUploadModal();
            }, 1200);
        }, 600);
    } else {
        fileNameDisplay.textContent = '';
        uploadBox.classList.remove('file-selected');
    }
});

document.getElementById('resume-file')?.addEventListener('change', function(e) {
    console.log('Resume file change event triggered');
    const file = e.target.files[0];
    const fileNameDisplay = document.getElementById('resume-file-name');
    const uploadBox = this.closest('.upload-box');

    if (file) {
        const fileName = file.name;
        console.log('File selected:', fileName);

        // Show modal IMMEDIATELY - no delays!
        console.log('Showing modal now...');
        showUploadModal('Uploading Document...', `Please wait, "${fileName}" is being uploaded...`, false);

        // Brief processing delay to show buffer
        setTimeout(() => {
            console.log('Showing success...');
            // Show success
            showUploadModal('Upload Complete!', `"${fileName}" has been successfully uploaded and is ready to use.`, true);

            // Update UI
            fileNameDisplay.textContent = '✓ ' + fileName;
            uploadBox.classList.add('file-selected');

            // Auto-close modal
            setTimeout(() => {
                console.log('Hiding modal...');
                hideUploadModal();
            }, 1200);
        }, 600);
    } else {
        fileNameDisplay.textContent = '';
        uploadBox.classList.remove('file-selected');
    }
});

// File upload listener for multiple-rfrs resume file
document.getElementById('multiple-rfrs-resume-file')?.addEventListener('change', function(e) {
    const fileName = e.target.files[0]?.name || '';
    const fileNameDisplay = document.getElementById('multiple-rfrs-resume-name');
    const uploadBox = this.closest('.upload-box');

    if (fileName) {
        fileNameDisplay.textContent = '✓ ' + fileName;
        uploadBox.classList.add('file-selected');
    } else {
        fileNameDisplay.textContent = '';
        uploadBox.classList.remove('file-selected');
    }
});

// ==================== BROWSE FILE/FOLDER FUNCTIONS ====================
function browseFile(inputId) {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf';
    input.onchange = (e) => {
        const file = e.target.files[0];
        if (file) {
            document.getElementById(inputId).value = file.path || file.name;
        }
    };
    input.click();
}

function browseFolder(inputId) {
    const input = document.createElement('input');
    input.type = 'file';
    input.webkitdirectory = true;
    input.directory = true;
    input.onchange = (e) => {
        const files = e.target.files;
        if (files.length > 0) {
            const path = files[0].path || files[0].webkitRelativePath;
            const folderPath = path.substring(0, path.lastIndexOf('/'));
            document.getElementById(inputId).value = folderPath;
        }
    };
    input.click();
}

// ==================== FORM SUBMISSIONS ====================
// Single resume form submission
document.getElementById('single-form')?.addEventListener('submit', async function(e) {
    e.preventDefault();

    const formData = new FormData(this);
    const submitBtn = this.querySelector('.btn-primary');
    const loader = document.getElementById('single-loader');

    // Get file names for display in duplicate modal
    const rfrFileName = formData.get('rfr')?.name || 'Unknown RFR';
    const resumeFileName = formData.get('resume')?.name || 'Unknown Resume';

    // Show uploading message first
    showLoading('Uploading Files...', 'Please wait while files are being uploaded');
    loader.classList.remove('d-none');
    submitBtn.disabled = true;
    hideError();
    hideSuccess();

    // Track message timeout to cancel if request completes quickly (cached response)
    let messageTimeout = null;

    try {
        // Change message to processing after a brief moment, but only if request is still pending
        messageTimeout = setTimeout(() => {
            showLoading('Evaluating Resume', 'AI is analyzing RFR and resume...');
        }, 500);

        const response = await fetch('/pdf/evaluate', {
            method: 'POST',
            body: formData
        });

        // Clear the timeout since we got a response
        if (messageTimeout) {
            clearTimeout(messageTimeout);
        }

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // Check if this is a cached evaluation
        const isCached = response.headers.get('X-Evaluation-Cached') === 'true';
        const rfrName = response.headers.get('X-RFR-Name') || rfrFileName;
        const resumeName = response.headers.get('X-Resume-Name') || resumeFileName;

        const result = await response.text();

        // Validate the response is valid JSON or expected format
        if (!result || result.trim() === '') {
            throw new Error('Empty response from server');
        }

        await displaySingleResult(result);

        // Hide loading overlay first
        hideLoading();

        // If cached, show duplicate modal instead of success message
        if (isCached) {
            showDuplicateModal(rfrName, resumeName);
        } else {
            // Clear form after successful evaluation
            resetFileInput('rfr-file', 'rfr-file-name', '.upload-box');
            resetFileInput('resume-file', 'resume-file-name', '.upload-box');

            // Show success message
            showSuccess('Evaluation completed successfully! Forms have been cleared for next evaluation.');

            // Scroll to results
            document.getElementById('evaluations-section')?.scrollIntoView({ behavior: 'smooth' });
        }
    } catch (error) {
        console.error('Evaluation error:', error);
        showError('Failed to evaluate resume: ' + error.message);
    } finally {
        // Ensure timeout is cleared
        if (messageTimeout) {
            clearTimeout(messageTimeout);
        }
        hideLoading();
        loader.classList.add('d-none');
        submitBtn.disabled = false;
    }
});

// Batch form submission
document.getElementById('batch-form')?.addEventListener('submit', async function(e) {
    e.preventDefault();

    const formData = new FormData(this);
    const submitBtn = this.querySelector('.btn-primary');
    const loader = document.getElementById('batch-loader');

    const rfrPath = formData.get('rfrPath');
    const resumesFolderPath = formData.get('resumesFolderPath');

    // Show uploading message
    showLoading('Processing Request...', 'Reading files from specified paths...');
    loader.classList.remove('d-none');
    submitBtn.disabled = true;
    hideError();
    hideSuccess();

    // Track loading state change timeout
    let loadingTimeout = null;

    try {
        loadingTimeout = setTimeout(() => {
            showLoading('Processing Multiple Resumes', 'Evaluating all resumes in the folder...');
        }, 500);

        const response = await fetch('/pdf/evaluate-folder', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                rfrPath: rfrPath,
                resumesFolderPath: resumesFolderPath
            })
        });

        // Clear loading timeout since we got a response
        if (loadingTimeout) {
            clearTimeout(loadingTimeout);
        }

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        await displayBatchResults(result);

        // Clear form after successful evaluation
        resetForm('batch-form');

        // Show success message with failure count if any
        const successMsg = result.failedCount > 0
            ? `Batch evaluation completed! ${result.successCount} successful, ${result.failedCount} failed out of ${result.totalResumes} resumes. Forms cleared.`
            : `Batch evaluation completed! ${result.totalResumes} resumes processed. Forms cleared.`;
        showSuccess(successMsg);

        // Scroll to results
        document.getElementById('evaluations-section')?.scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        showError('Failed to process resumes: ' + error.message);
    } finally {
        // Ensure timeout is cleared
        if (loadingTimeout) {
            clearTimeout(loadingTimeout);
        }
        hideLoading();
        loader.classList.add('d-none');
        submitBtn.disabled = false;
    }
});

// Multiple RFRs form submission
document.getElementById('multiple-rfrs-form')?.addEventListener('submit', async function(e) {
    e.preventDefault();

    const formData = new FormData(this);
    const submitBtn = this.querySelector('.btn-primary');
    const loader = document.getElementById('multiple-rfrs-loader');

    const rfrsFolderPath = document.getElementById('rfrs-folder-path').value;

    // IMPORTANT: Remove rfrsFolderPath from formData to avoid duplication
    // We're sending it in query params instead
    formData.delete('rfrsFolderPath');

    // Show uploading message
    showLoading('Uploading Resume...', 'Please wait while file is being uploaded');
    loader.classList.remove('d-none');
    submitBtn.disabled = true;
    hideError();
    hideSuccess();

    // Track loading state change timeout
    let loadingTimeout = null;

    try {
        loadingTimeout = setTimeout(() => {
            showLoading('Evaluating Against Multiple RFRs', 'Analyzing resume against all RFR documents...');
        }, 500);

        const queryParams = new URLSearchParams({
            rfrsFolderPath: rfrsFolderPath
        });

        const response = await fetch(`/pdf/evaluate-multiple-rfrs?${queryParams}`, {
            method: 'POST',
            body: formData  // Now only contains the resume file, not the path
        });

        // Clear loading timeout since we got a response
        if (loadingTimeout) {
            clearTimeout(loadingTimeout);
        }

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        await displayMultipleRfrsResults(result);

        // Clear form after successful evaluation
        document.getElementById('rfrs-folder-path').value = '';
        resetFileInput('multiple-rfrs-resume-file', 'multiple-rfrs-resume-name', '.upload-box');

        // Show success message
        const successMsg = result.failedCount > 0
            ? `Evaluation completed! ${result.successCount} successful, ${result.failedCount} failed out of ${result.totalRFRs} RFRs. Forms cleared.`
            : `Evaluation completed! Resume evaluated against ${result.totalRFRs} RFRs. Forms cleared.`;
        showSuccess(successMsg);

        // Scroll to results
        document.getElementById('evaluations-section')?.scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        showError('Failed to evaluate against multiple RFRs: ' + error.message);
    } finally {
        // Ensure timeout is cleared
        if (loadingTimeout) {
            clearTimeout(loadingTimeout);
        }
        hideLoading();
        loader.classList.add('d-none');
        submitBtn.disabled = false;
    }
});

// Bulk evaluation form submission
document.getElementById('bulk-form')?.addEventListener('submit', async function(e) {
    e.preventDefault();

    const formData = new FormData(this);
    const submitBtn = this.querySelector('.btn-primary');
    const loader = document.getElementById('bulk-loader');

    const rfrsFolderPath = formData.get('rfrsFolderPath');
    const resumesFolderPath = formData.get('resumesFolderPath');

    // Show processing message
    showLoading('Processing Request...', 'Reading files from specified paths...');
    loader.classList.remove('d-none');
    submitBtn.disabled = true;
    hideError();
    hideSuccess();

    // Track loading state change timeout
    let loadingTimeout = null;

    try {
        loadingTimeout = setTimeout(() => {
            showLoading('Bulk Evaluation in Progress', 'Processing all RFR and resume combinations...');
        }, 500);

        const response = await fetch('/pdf/evaluate-bulk', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                rfrsFolderPath: rfrsFolderPath,
                resumesFolderPath: resumesFolderPath
            })
        });

        // Clear loading timeout since we got a response
        if (loadingTimeout) {
            clearTimeout(loadingTimeout);
        }

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        await displayBulkResults(result);

        // Clear form after successful evaluation
        resetForm('bulk-form');

        // Show success message
        showSuccess(`Bulk evaluation completed! ${result.totalCombinations} evaluations processed. Forms cleared.`);

        // Scroll to results
        document.getElementById('evaluations-section')?.scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        showError('Failed to perform bulk evaluation: ' + error.message);
    } finally {
        // Ensure timeout is cleared
        if (loadingTimeout) {
            clearTimeout(loadingTimeout);
        }
        hideLoading();
        loader.classList.add('d-none');
        submitBtn.disabled = false;
    }
});

// ==================== DISPLAY FUNCTIONS ====================
// Display multiple RFRs results
async function displayMultipleRfrsResults(data) {
    console.log('Multiple RFRs Results:', data);
    await fetchAndDisplayAllEvaluations();
}

// Display bulk results
async function displayBulkResults(data) {
    console.log('Bulk Results:', data);
    await fetchAndDisplayAllEvaluations();
}

// Display single result
async function displaySingleResult(result) {
    await fetchAndDisplayAllEvaluations();
}

// Display batch results
async function displayBatchResults(data) {
    await fetchAndDisplayAllEvaluations();
}

// ==================== ERROR HANDLING ====================
// Show error message
function showError(message) {
    const errorSection = document.getElementById('error-section');
    const errorText = document.getElementById('error-text');
    errorText.textContent = message;
    errorSection.classList.remove('d-none');
    errorSection.classList.add('show');
    errorSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// Hide error message
function hideError() {
    const errorSection = document.getElementById('error-section');
    errorSection.classList.remove('show');
    errorSection.classList.add('d-none');
}

// ==================== FETCH AND DISPLAY EVALUATIONS ====================
// Fetch and display all evaluations
async function fetchAndDisplayAllEvaluations() {
    try {
        const response = await fetch('/ai/gemini/evaluations');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const evaluations = await response.json();
        displayEvaluationsTable(evaluations);
    } catch (error) {
        console.error('Failed to fetch evaluations:', error);
        const evaluationsContent = document.getElementById('evaluations-content');
        if (evaluationsContent) {
            evaluationsContent.innerHTML = '<p class="no-data">Failed to load evaluations. Please refresh the page.</p>';
        }
    }
}

// Global sort state
let currentSortBy = 'rfrId';
let currentSortOrder = 'asc';
let selectedRfrFilter = 'all';

// Display evaluations in table format
function displayEvaluationsTable(evaluations) {
    const evaluationsContent = document.getElementById('evaluations-content');

    if (!evaluationsContent) {
        console.error('evaluations-content element not found');
        return;
    }

    if (!evaluations || evaluations.length === 0) {
        evaluationsContent.innerHTML = '<p class="no-data">No evaluations available yet. Start by evaluating a resume!</p>';
        return;
    }

    // Get unique RFR IDs for filter dropdown
    const uniqueRfrIds = [...new Set(evaluations.map(e => e.rfrId))].sort();

    // Helper function to truncate long filenames for dropdown display
    const truncateFilename = (filename, maxLength = 40) => {
        if (filename.length <= maxLength) return filename;
        const ext = filename.substring(filename.lastIndexOf('.'));
        const nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        const truncatedName = nameWithoutExt.substring(0, maxLength - ext.length - 3) + '...';
        return truncatedName + ext;
    };

    // Filter evaluations by selected RFR
    let filteredEvaluations = evaluations;
    if (selectedRfrFilter !== 'all') {
        filteredEvaluations = evaluations.filter(e => e.rfrId === selectedRfrFilter);
    }

    // Sort evaluations
    const sortedEvaluations = sortEvaluations(filteredEvaluations, currentSortBy, currentSortOrder);

    let html = `
        <div class="table-controls">
            <div class="control-group">
                <label>Filter by RFR: </label>
                <select id="rfr-filter" onchange="handleRfrFilterChange()">
                    <option value="all" ${selectedRfrFilter === 'all' ? 'selected' : ''}>All RFRs</option>
                    ${uniqueRfrIds.map(rfrId =>
                        `<option value="${rfrId}" ${selectedRfrFilter === rfrId ? 'selected' : ''} title="${rfrId}">${truncateFilename(rfrId)}</option>`
                    ).join('')}
                </select>
            </div>
            <div class="control-group">
                <label>Sort by: </label>
                <select id="sort-by" onchange="handleSortChange()">
                    <option value="rfrId" ${currentSortBy === 'rfrId' ? 'selected' : ''}>RFR ID</option>
                    <option value="overallScore" ${currentSortBy === 'overallScore' ? 'selected' : ''}>Overall Score</option>
                    <option value="status" ${currentSortBy === 'status' ? 'selected' : ''}>Status</option>
                </select>
                <select id="sort-order" onchange="handleSortChange()">
                    <option value="asc" ${currentSortOrder === 'asc' ? 'selected' : ''}>Ascending</option>
                    <option value="desc" ${currentSortOrder === 'desc' ? 'selected' : ''}>Descending</option>
                </select>
            </div>
        </div>
        <div class="results-summary">
            <p>Showing <strong>${sortedEvaluations.length}</strong> of <strong>${evaluations.length}</strong> evaluations
               (${sortedEvaluations.filter(e => e.status === 'SELECTED').length} Selected,
                ${sortedEvaluations.filter(e => e.status === 'REJECTED').length} Rejected)</p>
        </div>
        <div class="table-container">
            <table class="evaluations-table">
                <thead>
                    <tr>
                        <th>RFR ID</th>
                        <th>Candidate ID</th>
                        <th>Accuracy Score</th>
                        <th>Complexity Score</th>
                        <th>Overall Score</th>
                        <th>Status</th>
                        <th>Rejection Reason / Notes</th>
                        <th>Skills</th>
                    </tr>
                </thead>
                <tbody>
    `;

    sortedEvaluations.forEach(evaluation => {
        const skillsDisplay = formatSkills(evaluation.skillsJson);
        const statusClass = evaluation.status === 'SELECTED' ? 'status-selected' : 'status-rejected';
        const accuracyScore = evaluation.accuracyScore !== null && evaluation.accuracyScore !== undefined
            ? evaluation.accuracyScore.toFixed(1) + '%'
            : 'N/A';
        const complexityScore = evaluation.complexityScore !== null && evaluation.complexityScore !== undefined
            ? evaluation.complexityScore.toFixed(1) + '%'
            : 'N/A';
        const rejectionReason = evaluation.rejectionReason || (evaluation.status === 'SELECTED' ? 'Meets all criteria' : '-');

        html += `
            <tr>
                <td>${evaluation.rfrId}</td>
                <td>${evaluation.candidateId}</td>
                <td class="score">${accuracyScore}</td>
                <td class="score">${complexityScore}</td>
                <td class="score">${evaluation.overallScore.toFixed(2)}%</td>
                <td><span class="${statusClass}">${evaluation.status}</span></td>
                <td class="rejection-reason">${rejectionReason}</td>
                <td class="skills-cell">${skillsDisplay}</td>
            </tr>
        `;
    });

    html += `
                </tbody>
            </table>
        </div>
    `;

    evaluationsContent.innerHTML = html;
}

// Format skills JSON for display
function formatSkills(skillsJson) {
    if (!skillsJson) return '-';

    try {
        const skills = typeof skillsJson === 'string' ? JSON.parse(skillsJson) : skillsJson;

        if (Array.isArray(skills)) {
            return skills.map(skill => {
                const name = skill.skill_name || skill.name || 'Unknown';
                const match = skill.match_status || skill.status || 'N/A';
                return `<div class="skill-item"><strong>${name}:</strong> ${match}</div>`;
            }).join('');
        } else if (typeof skills === 'object' && skills !== null) {
            return Object.entries(skills).map(([skillName, skillData]) => {
                if (typeof skillData === 'object' && skillData !== null) {
                    const match = skillData.match !== undefined ? skillData.match : 'N/A';
                    const recency = skillData.recency !== undefined ? skillData.recency : 'N/A';
                    const frequency = skillData.frequency !== undefined ? skillData.frequency : 'N/A';

                    return `
                        <div class="skill-item">
                            <div class="skill-name"><strong>${skillName}</strong></div>
                            <div class="skill-details">
                                <span class="skill-metric">Match: <span class="metric-value">${match}</span></span>
                                <span class="skill-metric">Recency: <span class="metric-value">${recency}</span></span>
                                <span class="skill-metric">Frequency: <span class="metric-value">${frequency}</span></span>
                            </div>
                        </div>
                    `;
                } else {
                    return `<div class="skill-item"><strong>${skillName}:</strong> ${skillData}</div>`;
                }
            }).join('');
        }

        return JSON.stringify(skills);
    } catch (e) {
        console.error('Error formatting skills:', e);
        return String(skillsJson);
    }
}

// Sort evaluations
function sortEvaluations(evaluations, sortBy, order) {
    const sorted = [...evaluations].sort((a, b) => {
        let compareA = a[sortBy];
        let compareB = b[sortBy];

        // Handle string comparison for rfrId and candidateId
        if (typeof compareA === 'string') {
            compareA = compareA.toLowerCase();
            compareB = compareB.toLowerCase();
        }

        if (compareA < compareB) return order === 'asc' ? -1 : 1;
        if (compareA > compareB) return order === 'asc' ? 1 : -1;
        return 0;
    });

    return sorted;
}

// Handle sort change
async function handleSortChange() {
    currentSortBy = document.getElementById('sort-by').value;
    currentSortOrder = document.getElementById('sort-order').value;
    await fetchAndDisplayAllEvaluations();
}

// Handle RFR filter change
async function handleRfrFilterChange() {
    selectedRfrFilter = document.getElementById('rfr-filter').value;
    await fetchAndDisplayAllEvaluations();
}

// Load evaluations on page load
document.addEventListener('DOMContentLoaded', function() {
    fetchAndDisplayAllEvaluations();
});