package br.com.rotavital.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BolsaSangue {

    private String codigo; // BS-00001
    private String tipoSanguineo;
    private String componente; // hemácias, plaquetas, plasma
    private int volume; // ml
    private String status; // disponivel, reservada, distribuida, vencida, descartada

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataColeta;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataValidade;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataDistribuicao;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataDescarte;

    private String doadorId;
    private String hospitalDestinoId;
    private String responsavelDistribuicao;
    private String responsavelDescarte;
    private String requisicaoId;
    private boolean usoPrioritario;

    public BolsaSangue() {}

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTipoSanguineo() { return tipoSanguineo; }
    public void setTipoSanguineo(String tipoSanguineo) { this.tipoSanguineo = tipoSanguineo; }

    public String getComponente() { return componente; }
    public void setComponente(String componente) { this.componente = componente; }

    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = volume; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDataColeta() { return dataColeta; }
    public void setDataColeta(LocalDate dataColeta) { this.dataColeta = dataColeta; }

    public LocalDate getDataValidade() { return dataValidade; }
    public void setDataValidade(LocalDate dataValidade) { this.dataValidade = dataValidade; }

    public LocalDateTime getDataDistribuicao() { return dataDistribuicao; }
    public void setDataDistribuicao(LocalDateTime dataDistribuicao) { this.dataDistribuicao = dataDistribuicao; }

    public LocalDateTime getDataDescarte() { return dataDescarte; }
    public void setDataDescarte(LocalDateTime dataDescarte) { this.dataDescarte = dataDescarte; }

    public String getDoadorId() { return doadorId; }
    public void setDoadorId(String doadorId) { this.doadorId = doadorId; }

    public String getHospitalDestinoId() { return hospitalDestinoId; }
    public void setHospitalDestinoId(String hospitalDestinoId) { this.hospitalDestinoId = hospitalDestinoId; }

    public String getResponsavelDistribuicao() { return responsavelDistribuicao; }
    public void setResponsavelDistribuicao(String responsavelDistribuicao) { this.responsavelDistribuicao = responsavelDistribuicao; }

    public String getResponsavelDescarte() { return responsavelDescarte; }
    public void setResponsavelDescarte(String responsavelDescarte) { this.responsavelDescarte = responsavelDescarte; }

    public String getRequisicaoId() { return requisicaoId; }
    public void setRequisicaoId(String requisicaoId) { this.requisicaoId = requisicaoId; }

    public boolean isUsoPrioritario() { return usoPrioritario; }
    public void setUsoPrioritario(boolean usoPrioritario) { this.usoPrioritario = usoPrioritario; }
}
