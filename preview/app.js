const INITIAL_PIECES = {
  '0,0':'♜','0,1':'♞','0,2':'♝','0,3':'♛','0,4':'♚','0,5':'♝','0,6':'♞','0,7':'♜',
  '1,0':'♟','1,1':'♟','1,2':'♟','1,3':'♟','1,4':'♟','1,5':'♟','1,6':'♟','1,7':'♟',
  '4,4':'♙',
  '6,0':'♙','6,1':'♙','6,2':'♙','6,3':'♙','6,5':'♙','6,6':'♙','6,7':'♙',
  '7,0':'♖','7,1':'♘','7,2':'♗','7,3':'♕','7,4':'♔','7,5':'♗','7,6':'♘','7,7':'♖'
};
const files = ['a','b','c','d','e','f','g','h'];
const boardEl = document.getElementById('board');

for (let r = 0; r < 8; r++) {
  for (let c = 0; c < 8; c++) {
    const sq = document.createElement('div');
    const isLight = (r + c) % 2 === 0;
    let cls = `sq ${isLight ? 'light' : 'dark'}`;
    if (r === 4 && c === 4) cls += ' last-move';
    if (r === 6 && c === 4) cls += ' last-move';
    sq.className = cls;

    if (c === 0) sq.innerHTML += `<span class="coord coord-rank">${8 - r}</span>`;
    if (r === 7) sq.innerHTML += `<span class="coord coord-file">${files[c]}</span>`;

    const piece = INITIAL_PIECES[`${r},${c}`];
    if (piece) {
      const isWhite = r >= 4;
      sq.innerHTML += `<span class="${isWhite ? 'piece-w' : 'piece-b'}">${piece}</span>`;
    }
    boardEl.appendChild(sq);
  }
}

function switchTab(tabId) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.view-section').forEach(s => s.classList.remove('active'));
  if (tabId === 'lobby') {
    document.querySelectorAll('.tab-btn')[0].classList.add('active');
    document.getElementById('view-lobby').classList.add('active');
  } else if (tabId === 'game') {
    document.querySelectorAll('.tab-btn')[1].classList.add('active');
    document.getElementById('view-game').classList.add('active');
  } else if (tabId === 'promo') {
    document.querySelectorAll('.tab-btn')[2].classList.add('active');
    document.getElementById('view-promo').classList.add('active');
  }
}
