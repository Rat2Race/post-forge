package com.postforge.api.lostark;

import com.postforge.domain.lostark.AccountInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lostark")
@RequiredArgsConstructor
public class LostarkController {

    private final LostarkService lostarkService;

    @GetMapping("/user/inform/{characterName}")
    public ResponseEntity<List<AccountInfo>> getUserInform(@PathVariable("characterName") String characterName) {
        return ResponseEntity.status(HttpStatus.OK).body(lostarkService.getAccountInform(characterName));
    }

}
