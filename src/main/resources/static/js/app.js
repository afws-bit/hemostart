(() => {
  const TIPOS_ACEITOS = ["image/jpeg", "image/png", "application/pdf"];
  const TAMANHO_MAXIMO = 10 * 1024 * 1024;
  const TEMPO_LIMITE_MS = 90000;

  const $ = (id) => document.getElementById(id);
  const el = {
    telaEnvio: $("tela-envio"),
    telaCarregando: $("tela-carregando"),
    telaResultado: $("tela-resultado"),
    dropzone: $("dropzone"),
    arquivo: $("arquivo"),
    camera: $("camera"),
    btnArquivo: $("btn-arquivo"),
    btnCamera: $("btn-camera"),
    btnValidar: $("btn-validar"),
    btnRemover: $("btn-remover"),
    btnNova: $("btn-nova"),
    escolhido: $("arquivo-escolhido"),
    miniatura: $("miniatura"),
    nome: $("arquivo-nome"),
    tamanho: $("arquivo-tamanho"),
    erro: $("erro"),
    seloResultado: $("selo-resultado"),
    iconeResultado: $("icone-resultado"),
    statusResultado: $("status-resultado"),
    tituloResultado: $("titulo-resultado"),
    descricaoResultado: $("descricao-resultado"),
    listaMotivos: $("lista-motivos"),
    metaProtocolo: $("meta-protocolo"),
    metaArquivo: $("meta-arquivo"),
    metaData: $("meta-data"),
  };

  const VISUAL_STATUS = {
    APROVADO: {
      classe: "", icone: "✓", rotulo: "APROVADO",
      titulo: "Nota aprovada!",
      descricao: "Todas as regras foram atendidas. Seu pedido segue para pagamento.",
    },
    REPROVADO: {
      classe: "reprovado", icone: "✕", rotulo: "REPROVADO",
      titulo: "Não foi possível aprovar a nota",
      descricao: "Veja abaixo o que precisa ser corrigido e envie novamente.",
    },
    REVISAO_MANUAL: {
      classe: "revisao", icone: "!", rotulo: "EM REVISÃO",
      titulo: "Sua nota vai para revisão",
      descricao: "Um analista vai conferir o documento. Você será avisado do resultado.",
    },
  };

  let arquivoSelecionado = null;
  let urlMiniatura = null;

  function mostrarTela(nome) {
    el.telaEnvio.hidden = nome !== "envio";
    el.telaCarregando.hidden = nome !== "carregando";
    el.telaResultado.hidden = nome !== "resultado";
  }

  function mostrarErro(mensagem) {
    el.erro.textContent = mensagem;
    el.erro.hidden = false;
  }

  function limparErro() {
    el.erro.hidden = true;
    el.erro.textContent = "";
  }

  function formatarTamanho(bytes) {
    if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  function liberarMiniatura() {
    if (urlMiniatura) {
      URL.revokeObjectURL(urlMiniatura);
      urlMiniatura = null;
    }
  }

  function selecionarArquivo(arquivo) {
    limparErro();
    if (!arquivo) return;

    if (!TIPOS_ACEITOS.includes(arquivo.type)) {
      mostrarErro("Formato não suportado. Envie JPG, PNG ou PDF.");
      return;
    }
    if (arquivo.size > TAMANHO_MAXIMO) {
      mostrarErro("O arquivo é maior que 10 MB. Envie um arquivo menor.");
      return;
    }

    arquivoSelecionado = arquivo;
    el.nome.textContent = arquivo.name;
    el.tamanho.textContent = formatarTamanho(arquivo.size);

    liberarMiniatura();
    el.miniatura.replaceChildren();
    if (arquivo.type.startsWith("image/")) {
      urlMiniatura = URL.createObjectURL(arquivo);
      const img = document.createElement("img");
      img.src = urlMiniatura;
      img.alt = "";
      el.miniatura.append(img);
    } else {
      el.miniatura.textContent = "PDF";
    }

    el.escolhido.hidden = false;
    el.btnValidar.disabled = false;
  }

  function removerArquivo() {
    arquivoSelecionado = null;
    el.arquivo.value = "";
    el.camera.value = "";
    liberarMiniatura();
    el.miniatura.replaceChildren();
    el.escolhido.hidden = true;
    el.btnValidar.disabled = true;
    limparErro();
  }

  async function enviar() {
    if (!arquivoSelecionado) return;
    limparErro();
    mostrarTela("carregando");

    const dados = new FormData();
    dados.append("arquivo", arquivoSelecionado);

    const controle = new AbortController();
    const temporizador = setTimeout(() => controle.abort(), TEMPO_LIMITE_MS);

    try {
      const resposta = await fetch("/reembolsos/validar", {
        method: "POST",
        body: dados,
        signal: controle.signal,
      });
      const corpo = await resposta.json().catch(() => null);

      if (!resposta.ok) {
        throw new Error(corpo?.erro || "Não foi possível validar a nota agora. Tente novamente.");
      }

      mostrarResultado(corpo);
      mostrarTela("resultado");
    } catch (e) {
      mostrarTela("envio");
      if (e.name === "AbortError") {
        mostrarErro("A análise demorou mais que o esperado. Tente novamente.");
      } else if (e instanceof TypeError) {
        mostrarErro("Não foi possível conectar ao servidor. Verifique sua conexão.");
      } else {
        mostrarErro(e.message);
      }
    } finally {
      clearTimeout(temporizador);
    }
  }

  function mostrarResultado(resultado) {
    const visual = VISUAL_STATUS[resultado.status] || VISUAL_STATUS.REVISAO_MANUAL;

    el.telaResultado.className = `cartao resultado ${visual.classe}`.trim();
    el.iconeResultado.textContent = visual.icone;
    el.statusResultado.textContent = visual.rotulo;
    el.tituloResultado.textContent = visual.titulo;
    el.descricaoResultado.textContent = visual.descricao;

    el.listaMotivos.replaceChildren();
    (resultado.motivos || []).forEach((motivo) => {
      const item = document.createElement("li");
      item.textContent = motivo;
      el.listaMotivos.append(item);
    });
    el.listaMotivos.hidden = el.listaMotivos.children.length === 0;

    el.metaProtocolo.textContent = `#${String(resultado.id).padStart(6, "0")}`;
    el.metaArquivo.textContent = arquivoSelecionado ? arquivoSelecionado.name : "-";
    el.metaData.textContent = resultado.criadoEm
      ? new Date(resultado.criadoEm).toLocaleString("pt-BR")
      : "-";
  }

  function novaNota() {
    removerArquivo();
    mostrarTela("envio");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  // Eventos
  el.dropzone.addEventListener("click", () => el.arquivo.click());
  el.dropzone.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      el.arquivo.click();
    }
  });
  el.btnArquivo.addEventListener("click", () => el.arquivo.click());
  el.btnCamera.addEventListener("click", () => el.camera.click());
  el.arquivo.addEventListener("change", () => selecionarArquivo(el.arquivo.files[0]));
  el.camera.addEventListener("change", () => selecionarArquivo(el.camera.files[0]));

  ["dragenter", "dragover"].forEach((nome) =>
    el.dropzone.addEventListener(nome, (e) => {
      e.preventDefault();
      el.dropzone.classList.add("arrastando");
    })
  );
  ["dragleave", "drop"].forEach((nome) =>
    el.dropzone.addEventListener(nome, (e) => {
      e.preventDefault();
      el.dropzone.classList.remove("arrastando");
    })
  );
  el.dropzone.addEventListener("drop", (e) => selecionarArquivo(e.dataTransfer.files[0]));

  el.btnRemover.addEventListener("click", removerArquivo);
  el.btnValidar.addEventListener("click", enviar);
  el.btnNova.addEventListener("click", novaNota);
})();
