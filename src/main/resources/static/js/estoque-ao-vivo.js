/*
 * RSD (U2) — alertas de estoque crítico em tempo real.
 *
 * Abre um WebSocket em /ws/estoque (wss:// quando a página está em HTTPS, como no Render) e:
 *   - atualiza a lista de alertas (#alertas-estoque) sem recarregar a página;
 *   - atualiza os cards por tipo do painel de estoque (elementos com data-tipo);
 *   - mostra um aviso no canto da tela quando um tipo entra em crítico ou volta ao normal.
 * Se a conexão cair, tenta de novo com espera crescente (1 s, 2 s, 4 s ... até 30 s).
 */
(function () {
    'use strict';

    var lista = document.getElementById('alertas-estoque');
    var status = document.getElementById('ws-status');
    if (!lista && !document.querySelector('[data-tipo]')) {
        return; // página sem nada para atualizar
    }

    var esperaMs = 1000;

    function definirStatus(texto, classe) {
        if (!status) return;
        status.textContent = texto;
        status.className = 'ws-status ' + classe;
    }

    function escapar(texto) {
        var div = document.createElement('div');
        div.textContent = String(texto);
        return div.innerHTML;
    }

    function renderizarLista(situacao) {
        if (!lista) return;
        var criticos = situacao.filter(function (s) { return s.critico; });
        if (criticos.length === 0) {
            lista.innerHTML = '<p class="ws-vazio">Nenhum alerta de estoque crítico no momento.</p>';
            return;
        }
        lista.innerHTML = criticos.map(function (s) {
            return '<div class="alerta-critico">⚠️ Estoque crítico para <strong>' + escapar(s.tipo) +
                '</strong> — ' + escapar(s.disponivel) + ' disponível(is), mínimo ' + escapar(s.minimo) + '</div>';
        }).join('');
    }

    function atualizarCards(situacao) {
        situacao.forEach(function (s) {
            var card = document.querySelector('[data-tipo="' + s.tipo + '"]');
            if (!card) return;
            card.classList.toggle('critico', s.critico);
            var disp = card.querySelector('[data-campo="disponivel"]');
            if (disp) disp.textContent = 'Disp: ' + s.disponivel;
            var aviso = card.querySelector('[data-campo="aviso"]');
            if (aviso) aviso.style.display = s.critico ? '' : 'none';
        });
    }

    function avisar(mudanca) {
        var aviso = document.createElement('div');
        var critico = mudanca.evento === 'ENTROU_EM_CRITICO';
        aviso.className = 'ws-aviso ' + (critico ? 'ws-aviso-critico' : 'ws-aviso-normal');
        aviso.innerHTML = critico
            ? '⚠️ <strong>' + escapar(mudanca.tipo) + '</strong> entrou em estoque crítico: ' +
              escapar(mudanca.disponivel) + ' disponível(is), mínimo ' + escapar(mudanca.minimo)
            : '✅ Estoque de <strong>' + escapar(mudanca.tipo) + '</strong> voltou ao normal: ' +
              escapar(mudanca.disponivel) + ' disponível(is)';
        var area = document.getElementById('ws-avisos');
        if (!area) {
            area = document.createElement('div');
            area.id = 'ws-avisos';
            document.body.appendChild(area);
        }
        area.appendChild(aviso);
        setTimeout(function () { aviso.remove(); }, 8000);
    }

    function conectar() {
        var protocolo = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
        var ws = new WebSocket(protocolo + window.location.host + '/ws/estoque');
        definirStatus('● conectando…', 'ws-conectando');

        ws.onopen = function () {
            esperaMs = 1000;
            definirStatus('● ao vivo', 'ws-ao-vivo');
        };

        ws.onmessage = function (evento) {
            var msg;
            try { msg = JSON.parse(evento.data); } catch (e) { return; }
            if (!msg || msg.evento !== 'estoque') return;
            renderizarLista(msg.situacao);
            atualizarCards(msg.situacao);
            (msg.mudancas || []).forEach(avisar);
        };

        ws.onclose = function () {
            definirStatus('● reconectando…', 'ws-offline');
            setTimeout(conectar, esperaMs);
            esperaMs = Math.min(esperaMs * 2, 30000);
        };
    }

    if ('WebSocket' in window) {
        conectar();
    }
})();
