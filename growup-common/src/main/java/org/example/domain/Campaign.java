package org.example.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@EntityListeners(AuditingEntityListener.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String camCampaignName;

    private String camAdType;

    @Builder.Default
    private Boolean camOpen = false;

    @Builder.Default
    @OneToMany(mappedBy = "campaign",fetch = FetchType.LAZY)
    private List<Keyword> keywordList = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "email", referencedColumnName = "email")
    private Member member;
}