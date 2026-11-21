(() => {
  const normalize = (value) => (value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('vi');

  const enhanceTable = (table) => {
    if (table.dataset.adminTools === 'ready' || table.closest('.calendar-grid, .admin-no-tools')) return;
    const tbody = table.tBodies[0];
    if (!tbody) return;
    const rows = Array.from(tbody.rows).filter(row => !row.querySelector('td[colspan]'));
    if (rows.length === 0) return;
    table.dataset.adminTools = 'ready';

    const wrapper = table.closest('.table-responsive') || table.parentElement;
    if (!table.closest('.table-responsive')) {
      const responsive = document.createElement('div');
      responsive.className = 'table-responsive';
      table.parentNode.insertBefore(responsive, table);
      responsive.append(table);
    }

    const controls = document.createElement('div');
    controls.className = 'admin-table-tools';
    const search = document.createElement('input');
    search.type = 'search';
    search.className = 'form-control';
    search.style.width = 'min(25rem, 100%)';
    search.placeholder = 'Tìm trong danh sách…';
    search.setAttribute('aria-label', 'Tìm trong bảng');

    const badgeLabels = [...new Set(rows.flatMap(row => Array.from(row.querySelectorAll('.badge')).map(badge => badge.textContent.trim())).filter(Boolean))];
    let status = null;
    if (badgeLabels.length > 1 && badgeLabels.length <= 12) {
      status = document.createElement('select');
      status.className = 'form-select';
      status.style.width = 'min(17rem, 100%)';
      status.setAttribute('aria-label', 'Lọc theo trạng thái');
      status.add(new Option('Tất cả trạng thái', ''));
      badgeLabels.forEach(label => status.add(new Option(label, label)));
    }

    const pageSize = document.createElement('select');
    pageSize.className = 'form-select';
    pageSize.style.width = 'auto';
    pageSize.setAttribute('aria-label', 'Số dòng mỗi trang');
    [10, 25, 50].forEach(size => pageSize.add(new Option(`${size} dòng`, String(size))));
    const count = document.createElement('span');
    count.className = 'admin-list-count ms-auto';
    controls.append(search);
    if (status) controls.append(status);
    controls.append(pageSize, count);
    wrapper.parentNode.insertBefore(controls, wrapper);

    const pagination = document.createElement('div');
    pagination.className = 'admin-list-pagination';
    wrapper.after(pagination);
    let page = 1;

    const render = () => {
      const query = normalize(search.value.trim());
      const selectedStatus = status?.value || '';
      const matching = rows.filter(row => {
        const matchesQuery = normalize(row.textContent).includes(query);
        const matchesStatus = !selectedStatus || Array.from(row.querySelectorAll('.badge')).some(badge => badge.textContent.trim() === selectedStatus);
        return matchesQuery && matchesStatus;
      });
      const size = Number(pageSize.value || 10);
      const pages = Math.max(1, Math.ceil(matching.length / size));
      page = Math.min(page, pages);
      rows.forEach(row => { row.style.display = 'none'; });
      matching.slice((page - 1) * size, page * size).forEach(row => { row.style.display = ''; });
      count.textContent = `Hiển thị ${matching.length ? (page - 1) * size + 1 : 0}–${Math.min(page * size, matching.length)} / ${matching.length}`;
      pagination.replaceChildren();
      if (pages <= 1) return;
      const addPageButton = (label, target, active = false, disabled = false) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = `btn btn-sm ${active ? 'btn-primary' : 'btn-outline-secondary'}`;
        button.textContent = label;
        button.disabled = disabled;
        button.setAttribute('aria-label', label === '‹' ? 'Trang trước' : label === '›' ? 'Trang sau' : `Trang ${label}`);
        button.addEventListener('click', () => { page = target; render(); });
        pagination.append(button);
      };
      addPageButton('‹', page - 1, false, page === 1);
      for (let number = Math.max(1, page - 2); number <= Math.min(pages, page + 2); number++) addPageButton(String(number), number, number === page);
      addPageButton('›', page + 1, false, page === pages);
    };
    search.addEventListener('input', () => { page = 1; render(); });
    status?.addEventListener('change', () => { page = 1; render(); });
    pageSize.addEventListener('change', () => { page = 1; render(); });
    render();
  };

  const init = () => {
    document.querySelectorAll('body main table').forEach(enhanceTable);
    document.querySelectorAll('body main form').forEach(form => {
      form.addEventListener('submit', event => {
        if (!form.checkValidity()) {
          event.preventDefault();
          event.stopPropagation();
        }
        form.classList.add('was-validated');
      });
    });
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, { once: true });
  else init();
})();
