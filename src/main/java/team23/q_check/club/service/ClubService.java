package team23.q_check.club.service;

import org.springframework.stereotype.Service;
import team23.q_check.club.dto.ClubResponseDto;

@Service
public class ClubService {

    public ClubResponseDto getSampleClub() {
        return new ClubResponseDto(1L, "UMC", "University MakeUs Challenge club");
    }
}
